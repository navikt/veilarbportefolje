package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.ArenaInnholdKafka;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GoldenGateDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.GoldenGateOperations;
import no.nav.pto.veilarbportefolje.domene.AktorClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

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
        try {
            return aktorClient.hentAktorId(Fnr.ofValidFnr(personident));
        } catch (RuntimeException exception) {
            if (isDevelopment().orElse(false)) {
                return AktorId.of("-1");
            }
            throw exception;
        }
    }


    static ZonedDateTime getDateOrNull(ArenaDato date){
        return getDateOrNull(date, false);
    }

    static ZonedDateTime getDateOrNull(ArenaDato date, boolean tilOgMedDato){
        if(date == null){
            return null;
        }
        if(tilOgMedDato){
            return date.getDato().plusHours(23).plusMinutes(59);
        }
        return date.getDato();
    }

    static LocalDateTime getLocalDateOrNull(ArenaDato date, boolean tilOgMedDato){
        if(date == null){
            return null;
        }
        if(tilOgMedDato){
            return date.getLocalDate().plusHours(23).plusMinutes(59);
        }
        return date.getLocalDate();
    }


    static boolean erUtgatt(ArenaDato tilDato, boolean tilOgMedDato) {
         if(tilDato == null){
             return false;
         }
         ZonedDateTime tilZonedDato = getDateOrNull(tilDato, tilOgMedDato);
         ZonedDateTime now = ZonedDateTime.now();
         return tilZonedDato.isBefore(now);
    }
}
