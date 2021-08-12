package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakStatuser;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static no.nav.pto.veilarbportefolje.arenaaktiviteter.ArenaAktivitetUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TiltakServiceV2 {
    private static final LocalDate LANSERING_AV_OVERSIKTEN = LocalDate.of(2017, 12, 4);
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    @NonNull
    @Qualifier("systemClient")
    private final AktorClient aktorClient;
    private final ArenaHendelseRepository arenaHendelseRepository;
    private final BrukerDataService brukerDataService;
    private final ElasticIndexer elasticIndexer;

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

    public void behandleKafkaMelding(TiltakDTO kafkaMelding) {
        TiltakInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        if (skalSlettesGoldenGate(kafkaMelding) || skalSlettesTiltak(innhold)) {
            log.info("Sletter tiltak: {}, pa aktoer: {}", innhold.getAktivitetid(), aktorId);
            tiltakRepositoryV2.delete(innhold.getAktivitetid());
        } else {
            log.info("Lagrer tiltak: {}, pa aktoer: {}", innhold.getAktivitetid(), aktorId);
            tiltakRepositoryV2.upsert(innhold, aktorId);
        }
        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(PersonId.of(String.valueOf(innhold.getPersonId())), aktorId);
        arenaHendelseRepository.upsertHendelse(innhold.getAktivitetid(), innhold.getHendelseId());
        brukerDataService.oppdaterAktivitetBrukerData(aktorId, PersonId.of(String.valueOf(innhold.getPersonId())));

        elasticIndexer.indekser(aktorId);
    }

    public EnhetTiltak hentEnhettiltak(EnhetId enhet) {
        return tiltakRepositoryV2.hentTiltakPaEnhet(enhet);
    }

    private boolean erGammelMelding(TiltakDTO kafkaMelding, TiltakInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveHendelse(innhold.getAktivitetid());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), kafkaMelding.getOperationType())) {
            log.info("Fikk tilsendt gammel tiltaks-melding");
            return true;
        }
        return false;
    }


    static boolean skalSlettesTiltak(TiltakInnhold tiltakInnhold) {
        List<String> godkjenteStatuser;
        if ("GRUPPEAMO" .equals(tiltakInnhold.getTiltakstype())) {
            godkjenteStatuser = TiltakStatuser.godkjenteTiltaksStatuser;
        } else {
            godkjenteStatuser = TiltakStatuser.godkjenteTiltaksStatuserGruppeAMO;
        }

        if (tiltakInnhold.getAktivitetperiodeTil() == null) {
            return !godkjenteStatuser.contains(tiltakInnhold.getDeltakerStatus());
        }
        return !godkjenteStatuser.contains(tiltakInnhold.getDeltakerStatus()) || LANSERING_AV_OVERSIKTEN.isAfter(tiltakInnhold.getAktivitetperiodeTil().getDato().toLocalDate());

    }
}
