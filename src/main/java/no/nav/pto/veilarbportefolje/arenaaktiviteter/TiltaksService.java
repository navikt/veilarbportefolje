package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakStatuser;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.arenaaktiviteter.ArenaAktivitetUtils.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TiltaksService {
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    @NonNull
    @Qualifier("systemClient")
    private final AktorClient aktorClient;
    private final ArenaHendelseRepository arenaHendelseRepository;

    public void behandleKafkaRecord(ConsumerRecord<String, TiltakDTO> kafkaMelding) {
        TiltakDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key {} og offset {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.topic()
        );
        behandleKafkaMelding(melding);
    }

    public void behandleKafkaMelding(TiltakDTO kafkaMelding) {
        log.info("Behandler utdannings-aktivtet-melding");
        TiltakInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        if (skalSlettesGoldenGate(kafkaMelding) || skalSlettesTiltak(innhold)) {
            tiltakRepositoryV2.delete(innhold.getAktivitetid());
            //aktivitetService.slettOgIndekserAktivitet(innhold.getAktivitetid(), aktorId);
        } else {
            Brukertiltak tiltak = mapTiltiltak(innhold);
            tiltakRepositoryV2.upsert(tiltak, aktorId, innhold.getAktivitetid());
        }
        arenaHendelseRepository.upsertHendelse(innhold.getAktivitetid(), innhold.getHendelseId());
    }

    private Brukertiltak mapTiltiltak(TiltakInnhold innhold) {
        Timestamp tilDato = Optional.ofNullable(getDateOrNull(innhold.getAktivitetperiodeTil(), true))
                                    .map(DateUtils::toTimestamp)
                                    .orElse(null);

        return Brukertiltak.of(Fnr.of(innhold.getFnr()), innhold.getTiltakstype(), tilDato);
    }

    /**
     * Har side effekt med a lagre hvilken arena meldinger som er lest i DB
     */
    private boolean erGammelMelding(TiltakDTO kafkaMelding, TiltakInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveHendelse(innhold.getAktivitetid());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), kafkaMelding.getOperationType())) {
            log.info("Fikk tilsendt gammel tiltaks-melding");
            return true;
        }
        return false;
    }

    static boolean skalSlettesTiltak(TiltakInnhold tiltakInnhold) {
        return tiltakInnhold.getAktivitetperiodeTil() == null || !TiltakStatuser.GJENNOMFORER.equals(tiltakInnhold.getDeltakerStatus());
    }
}
