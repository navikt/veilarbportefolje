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
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

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
    private final AktorClient aktorClient;
    private final ArenaHendelseRepository arenaHendelseRepository;
    private final OpensearchIndexer opensearchIndexer;

    private final Cache<EnhetId, EnhetTiltak> enhetTiltakCachePostgres = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public void behandleKafkaRecord(ConsumerRecord<String, TiltakDTO> kafkaMelding) {
        TiltakDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMelding(melding);
    }


    /*
     * Metoden er deprecated da vi nå henter data om tiltaksaktiviteter fra ny datakilde.
     * Metoden las stå inntil testene som bruker den er fjernet.
     *
     */
    @Deprecated
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
     * Behandler en KafkaAktivitetMelding som om det var en TiltakDTO.
     *
     * Denne metoden er en midlertidig tilpasning slik at vi kan ta i mot data om tiltaksaktiviteter
     * fra ny datakilde uten å måtte endre implementasjonen for persistering og uthenting/presentasjon
     * av disse dataene.
     *
     * Tidligere ble tiltaksaktiviteter lest fra KafkaConfigCommon.Topic.TILTAK_TOPIC,
     * men leses nå fra Aktivitetsplanen KafkaConfigCommon.Topic.AIVEN_AKTIVITER_TOPIC.
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
            tiltakRepositoryV2.delete(aktivitetId);
            return true;
        } else if (erNyVersjonAvAktivitet(kafkaMelding)) {
            log.info("Lagrer tiltak postgres: {}, pa aktoer: {}", aktivitetId, aktorId);
            tiltakRepositoryV2.upsert(mapTilTiltakinnhold(kafkaMelding), aktorId, kafkaMelding.getVersion());
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

        Long databaseVersjon = tiltakRepositoryV2.getVersjon(aktivitet.getAktivitetId());

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

    public static TiltakInnhold mapTilTiltakinnhold(KafkaAktivitetMelding kafkaMelding) {
        if (kafkaMelding == null) {
            return null;
        }

        return new TiltakInnhold()
                .setAktivitetid(kafkaMelding.getAktivitetId())
                .setAktivitetperiodeFra(ArenaDato.of(kafkaMelding.getFraDato()))
                .setAktivitetperiodeTil(ArenaDato.of(kafkaMelding.getTilDato()))
                .setTiltakstype(kafkaMelding.getTiltakskode())
                .setTiltaksnavn(TiltakkodeverkMapper.mapTilTiltaknavn(kafkaMelding.getTiltakskode()));
    }

    public EnhetTiltak hentEnhettiltak(EnhetId enhet) {
        return tryCacheFirst(enhetTiltakCachePostgres, enhet,
                () -> tiltakRepositoryV2.hentTiltakPaEnhet(enhet));
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
