package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.*;
import no.nav.pto.veilarbportefolje.domene.AktorClient;

import java.time.ZonedDateTime;

public interface ArenaAktivitetUtils {
     static <T extends ArenaInnholdKafka> T getInnhold(GoldenGateDTO<T> goldenGateDTO) {
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

    static boolean skalSlettesGoldenGate(GoldenGateDTO<?> kafkaMelding) {
        return GoldenGateOperations.DELETE.equals(kafkaMelding.getOperationType());
    }

    static boolean erGammelHendelseBasertPaOperasjon(Long hendelseFraDB, Long hendelseFraKafka, String operation) {
        if(hendelseFraDB == null){
            return false;
        }
        if(hendelseFraKafka == null){
            return true;
        }
        if(GoldenGateOperations.DELETE.equals(operation)){
            return hendelseFraKafka.compareTo(hendelseFraDB) < 0;
        }
        return hendelseFraKafka.compareTo(hendelseFraDB) < 1;
    }

    static AktorId getAktorId(AktorClient aktorClient, String personident) {
        return aktorClient.hentAktorId(Fnr.ofValidFnr(personident));
    }


    static ZonedDateTime getDateOrNull(ArenaDato date){
        return getDateOrNull(date, false);
    }

    static ZonedDateTime getDateOrNull(ArenaDato date, boolean tilOgMedDato){
        if(date == null){
            return null;
        }
        if(tilOgMedDato){
            return date.getDato().plusDays(1);
        }
        return date.getDato();
    }
}
