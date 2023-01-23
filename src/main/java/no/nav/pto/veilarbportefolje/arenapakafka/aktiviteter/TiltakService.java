package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.TiltakStatuser;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Map.entry;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.erGammelHendelseBasertPaOperasjon;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getAktorId;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getInnhold;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.skalSlettesGoldenGate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltakService {
    private static final LocalDate LANSERING_AV_OVERSIKTEN = LocalDate.of(2017, 12, 4);
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final TiltakRepositoryV3 tiltakRepositoryV3;
    private final AktorClient aktorClient;
    private final ArenaHendelseRepository arenaHendelseRepository;
    private final OpensearchIndexer opensearchIndexer;
    private final UnleashService unleashService;

    private final Cache<EnhetId, EnhetTiltak> enhetTiltakCachePostgres = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private final List<String> ekskluderteTiltakstyper = List.of("MIDLONTIL", "VARLONTIL");

    public void behandleKafkaRecord(ConsumerRecord<String, TiltakDTO> kafkaMelding) {
        TiltakDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );

        if (FeatureToggle.lonnstilskuddFraDABEnabled(unleashService)) {
            boolean erIkkeLonnstilskudd = !ekskluderteTiltakstyper.contains(melding.getAfter().getTiltakstype());
            if (erIkkeLonnstilskudd) {
                behandleKafkaMelding(melding);
            }
        } else {
            behandleKafkaMelding(melding);
        }
    }


    public void behandleKafkaMelding(TiltakDTO kafkaMelding) {
        TiltakInnhold innhold = getInnhold(kafkaMelding);

        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());

        if (skalSlettesGoldenGate(kafkaMelding) || skalSlettesTiltak(innhold)) {
            log.info("Sletter tiltak postgres: {}, pa aktoer: {}", innhold.getAktivitetid(), aktorId);
            tiltakRepositoryV2.delete(innhold.getAktivitetid());
        } else {
            log.info("Lagrer tiltak postgres: {}, pa aktoer: {}", innhold.getAktivitetid(), aktorId);
            tiltakRepositoryV2.upsert(innhold, aktorId);
        }

        arenaHendelseRepository.upsertAktivitetHendelse(innhold.getAktivitetid(), innhold.getHendelseId());
        opensearchIndexer.indekser(aktorId);
    }

    /*
     * Tidligere ble "Varig lønnstilskudd" og "Midlertidig lønnstilskudd" lest fra KafkaConfigCommon.Topic.TILTAK_TOPIC,
     * men leses nå fra Aktivitetsplanen sin KafkaConfigCommon.Topic.AIVEN_AKTIVITER_TOPIC.
     *
     * Vi velger å metoden i denne klassen siden det strengt tatt er overlappende domene med resten av tiltaksaktivitetene.
     *
     * Se forøvrig Arena-dokumentasjonen for Tiltaksaktivitet:
     * https://confluence.adeo.no/pages/viewpage.action?pageId=409961201#ARENA413103L%C3%B8sningsbeskrivelse-TILTAKSAKTIVITET
     */
    public boolean behandleKafkaMelding(KafkaAktivitetMelding kafkaMelding) {
        if (!validerMelding(kafkaMelding)) {
            return false;
        }

        AktorId aktorId = AktorId.of(kafkaMelding.getAktorId());
        String aktivitetId = kafkaMelding.getAktivitetId();

        if (kafkaMelding.isHistorisk()) {
            log.info("Sletter tiltak postgres: {}, pa aktoer: {}", aktivitetId, aktorId);
            tiltakRepositoryV3.delete(aktivitetId);
            return true;
        } else if (erNyVersjonAvAktivitet(kafkaMelding)) {
            log.info("Lagrer tiltak postgres: {}, pa aktoer: {}", aktivitetId, aktorId);
            tiltakRepositoryV3.upsert(mapTilTiltakaktivitetEntity(kafkaMelding), aktorId);
            return true;
        } else {
            return false;
        }
    }

    private boolean erNyVersjonAvAktivitet(KafkaAktivitetMelding aktivitet) {
        Long kommendeVersjon = aktivitet.getVersion();

        if (kommendeVersjon == null) {
            return false;
        }

        Long databaseVersjon = tiltakRepositoryV3.hentVersjon(aktivitet.getAktivitetId());

        if (databaseVersjon == null) {
            return true;
        }

        return kommendeVersjon.compareTo(databaseVersjon) >= 0;
    }


    private boolean validerMelding(KafkaAktivitetMelding kafkaMelding) {
        if (kafkaMelding == null) {
            log.warn("Ble tilsendt tom melding (null). Meldingen prosesseres ikke.");
            return false;
        }

        if (kafkaMelding.getAktivitetId() == null) {
            log.warn("Ble tilsendt uten aktivitetId. Meldingen prosesseres ikke.");
            return false;
        }

        return true;
    }

    public static TiltakaktivitetEntity mapTilTiltakaktivitetEntity(KafkaAktivitetMelding kafkaMelding) {
        if (kafkaMelding == null) {
            return null;
        }

        return new TiltakaktivitetEntity()
                .setAktivitetId(kafkaMelding.getAktivitetId())
                .setFraDato(ArenaDato.of(kafkaMelding.getFraDato()))
                .setTilDato(ArenaDato.of(kafkaMelding.getTilDato()))
                .setTiltakskode(kafkaMelding.getTiltakskode())
                .setTiltaksnavn(TiltakkodeverkMapper.mapTilTiltaknavn(kafkaMelding.getTiltakskode()));
    }

    public EnhetTiltak hentEnhettiltak(EnhetId enhet) {
        return tryCacheFirst(enhetTiltakCachePostgres, enhet,
                () -> {
                    if (FeatureToggle.lonnstilskuddFraDABEnabled(unleashService)) {
                        return tiltakRepositoryV3.hentTiltakPaEnhet(enhet);
                    } else {
                        return tiltakRepositoryV2.hentTiltakPaEnhet(enhet);
                    }
                });
    }

    private boolean erGammelMelding(TiltakDTO kafkaMelding, TiltakInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveAktivitetHendelse(innhold.getAktivitetid());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            log.info("Fikk tilsendt gammel tiltaks-melding, aktivitet: {}", innhold.getAktivitetid());
            return true;
        }
        return false;
    }

    static boolean skalSlettesTiltak(TiltakInnhold tiltakInnhold) {
        if (tiltakInnhold.getAktivitetperiodeTil() == null) {
            return !TiltakStatuser.godkjenteTiltaksStatuser.contains(tiltakInnhold.getDeltakerStatus());
        }
        return !TiltakStatuser.godkjenteTiltaksStatuser.contains(tiltakInnhold.getDeltakerStatus()) || LANSERING_AV_OVERSIKTEN.isAfter(tiltakInnhold.getAktivitetperiodeTil().getDato().toLocalDate());
    }
}
