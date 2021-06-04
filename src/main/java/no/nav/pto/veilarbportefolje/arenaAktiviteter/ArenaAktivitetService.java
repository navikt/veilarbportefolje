package no.nav.pto.veilarbportefolje.arenaAktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.ArenaInnholdKafka;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.GoldenGateDTO;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.GoldenGateOperations;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.UtdanningsAktivitetInnhold;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaAktivitetService {
    private final AktorClient aktorClient;

    public KafkaAktivitetMelding mapTilKafkaAktivitetMelding(UtdanningsAktivitetInnhold melding, AktorId aktorId) {
        if(melding == null || aktorId == null){
            return null;
        }
        KafkaAktivitetMelding kafkaAktivitetMelding = new KafkaAktivitetMelding();
        kafkaAktivitetMelding.setAktorId(aktorId.get());
        kafkaAktivitetMelding.setAktivitetId(melding.getAktivitetid());
        kafkaAktivitetMelding.setAvtalt(true);
        kafkaAktivitetMelding.setVersion(-1L);
        kafkaAktivitetMelding.setFraDato(toZonedDateTime(melding.getAktivitetperiodeFra()));
        kafkaAktivitetMelding.setTilDato(toZonedDateTime(melding.getAktivitetperiodeTil()));
        kafkaAktivitetMelding.setEndretDato(toZonedDateTime(melding.getEndretDato()));

        return kafkaAktivitetMelding;
    }


    public ArenaInnholdKafka getInnhold(GoldenGateDTO goldenGateDTO) {
        switch (goldenGateDTO.getOperationType()) {
            case GoldenGateOperations.DELETE:
                return goldenGateDTO.getBefore();
            case GoldenGateOperations.INSERT:
            case GoldenGateOperations.UPDATE:
                return goldenGateDTO.getAfter();
            default:
                throw new IllegalArgumentException("Ukjent GoldenGate opperasjon");
        }
    }

    public boolean erGammelMelding(String id, long hendelseId) {
        return false; // TODO: finn en logikk som fungerer
    }

    public AktorId getAktorId(String personident) {
        return aktorClient.hentAktorId(Fnr.ofValidFnr(personident));
    }
}
