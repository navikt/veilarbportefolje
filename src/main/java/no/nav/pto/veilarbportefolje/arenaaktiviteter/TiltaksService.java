package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.TiltakStatuser;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.pto.veilarbportefolje.arenaaktiviteter.ArenaAktivitetUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TiltaksService {
    private final AktivitetService aktivitetService;
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
            aktivitetService.slettAktivitet(innhold.getAktivitetid(), aktorId);
        } else {
            KafkaAktivitetMelding melding = mapTilKafkaAktivitetMelding(innhold, aktorId);
            aktivitetService.upsertOgIndekserAktiviteter(melding);
        }
    }

    /**
        Har side effekt med a lagre hvilken arena meldinger som er lest i DB
     */
    private boolean erGammelMelding(TiltakDTO kafkaMelding, TiltakInnhold innhold ){
        Long hendelseIDB = arenaHendelseRepository.retrieveHendelse(innhold.getAktivitetid());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), kafkaMelding.getOperationType())) {
            log.info("Fikk tilsendt gammel tiltaks-melding");
            return true;
        }
        arenaHendelseRepository.upsertHendelse(innhold.getAktivitetid(), innhold.getHendelseId());
        return false;
    }

    static boolean skalSlettesTiltak(TiltakInnhold tiltakInnhold) {
        return tiltakInnhold.getAktivitetperiodeTil() == null || tiltakInnhold.getAktivitetperiodeFra() == null
                || !TiltakStatuser.GJENNOMFORER.name().equals(tiltakInnhold.getDeltakerStatus());
    }

    private KafkaAktivitetMelding mapTilKafkaAktivitetMelding(TiltakInnhold melding, AktorId aktorId) {
        if(melding == null || aktorId == null){
            return null;
        }
        KafkaAktivitetMelding kafkaAktivitetMelding = new KafkaAktivitetMelding();
        kafkaAktivitetMelding.setAktorId(aktorId.get());
        kafkaAktivitetMelding.setAktivitetId(melding.getAktivitetid()); //TODO: Sjekk om denne er unik i forhold til de andre
        kafkaAktivitetMelding.setFraDato(getDateOrNull(melding.getAktivitetperiodeFra()));
        kafkaAktivitetMelding.setTilDato(getDateOrNull(melding.getAktivitetperiodeTil(), true));
        kafkaAktivitetMelding.setEndretDato(getDateOrNull(melding.getEndretDato()));

        kafkaAktivitetMelding.setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);
        kafkaAktivitetMelding.setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK);
        kafkaAktivitetMelding.setAvtalt(true);
        kafkaAktivitetMelding.setHistorisk(false);
        kafkaAktivitetMelding.setVersion(-1L);

        return kafkaAktivitetMelding;
    }
}
