package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.ArenaInnholdKafka;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GoldenGateDTO;
import no.nav.pto.veilarbportefolje.domene.AktorClient;

import java.time.ZonedDateTime;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

public interface ArenaAktivitetUtils {
    static <T extends ArenaInnholdKafka> T getInnhold(GoldenGateDTO<T> goldenGateDTO) {
        if(skalSlettesGoldenGate(goldenGateDTO)){
            return goldenGateDTO.getBefore();
        }
        return goldenGateDTO.getAfter();
    }

    static boolean skalSlettesGoldenGate(GoldenGateDTO<?> kafkaMelding) {
        return kafkaMelding.getAfter() == null;
    }

    static boolean erGammelHendelseBasertPaOperasjon(Long hendelseFraDB, Long hendelseFraKafka, boolean skalSlettes) {
        if (hendelseFraDB == null) {
            return false;
        }
        if (hendelseFraKafka == null) {
            return true;
        }
        if (skalSlettes) {
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


    static ZonedDateTime getDateOrNull(ArenaDato date) {
        return getDateOrNull(date, false);
    }

    static ZonedDateTime getDateOrNull(ArenaDato date, boolean tilOgMedDato) {
        if (date == null) {
            return null;
        }
        if (tilOgMedDato) {
            return date.getDato().plusHours(23).plusMinutes(59);
        }
        return date.getDato();
    }

    static boolean erUtgatt(ArenaDato tilDato, boolean tilOgMedDato) {
        if (tilDato == null) {
            return false;
        }
        ZonedDateTime tilZonedDato = getDateOrNull(tilDato, tilOgMedDato);
        ZonedDateTime now = ZonedDateTime.now();
        return tilZonedDato.isBefore(now);
    }
}
