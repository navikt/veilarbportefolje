package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.*;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.UtdanningsAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.UtdanningsAktivitetInnhold;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.pto.veilarbportefolje.arenaaktiviteter.ArenaAktivitetUtils.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtdanningsAktivitetService {
    private final AktivitetService aktivitetService;
    private final AktorClient aktorClient;

    @Transactional
    public void behandleKafkaMelding(UtdanningsAktivitetDTO kafkaMelding) {
        log.info("Behandler utdannings-aktivtet-melding");
        UtdanningsAktivitetInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null) {
            return;
        }

        if (erGammelMelding(innhold.getAktivitetid(), innhold.getHendelseId())) {
            log.info("Fikk tilsendt gammel utdannings-aktivtet-melding");
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        if (skalSlettes(kafkaMelding)) {
            aktivitetService.slettAktivitet(innhold.getAktivitetid(), aktorId);
        } else {
            KafkaAktivitetMelding melding = mapTilKafkaAktivitetMelding(innhold, aktorId);
            aktivitetService.upsertOgIndekserAktiviteter(melding);
        }
    }

    private KafkaAktivitetMelding mapTilKafkaAktivitetMelding(UtdanningsAktivitetInnhold melding, AktorId aktorId) {
        if(melding == null || aktorId == null){
            return null;
        }
        KafkaAktivitetMelding kafkaAktivitetMelding = new KafkaAktivitetMelding();
        kafkaAktivitetMelding.setAktorId(aktorId.get());
        kafkaAktivitetMelding.setAktivitetId(melding.getAktivitetid()); //TODO: Sjekk om denne er unik i forhold til de andre
        kafkaAktivitetMelding.setFraDato(toZonedDateTime(melding.getAktivitetperiodeFra()));
        kafkaAktivitetMelding.setTilDato(toZonedDateTime(melding.getAktivitetperiodeTil()).plusDays(1)); // Til og med slutt dato
        kafkaAktivitetMelding.setEndretDato(toZonedDateTime(melding.getEndretDato()));

        kafkaAktivitetMelding.setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);
        kafkaAktivitetMelding.setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.UTDANNINGAKTIVITET);
        kafkaAktivitetMelding.setAvtalt(true);
        kafkaAktivitetMelding.setHistorisk(false);
        kafkaAktivitetMelding.setVersion(-1L);

        return kafkaAktivitetMelding;
    }
}
