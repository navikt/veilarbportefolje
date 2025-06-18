package no.nav.pto.veilarbportefolje.util;

import org.junit.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class FodselsnummerUtilsTest {
    private final String fodselsnummer = "10108000399"; //TESTFAMILIE
    private final String dnummer = "50108000399"; //TESTFAMILIE
    private final LocalDate fodselsdato = LocalDate.of(1980, 10, 10);

    @Test
    public void skalReturnereTrueHvisDNummer() {
        assertThat(FodselsnummerUtils.erDNummer(fodselsnummer)).isFalse();
        assertThat(FodselsnummerUtils.erDNummer(dnummer)).isTrue();
    }

    @Test
    public void skalKonvertereDNummerTilFodselsnummer() {
        assertThat(FodselsnummerUtils.konverterDNummerTilFodselsnummer(dnummer)).isEqualTo(fodselsnummer);
    }

    @Test
    public void skalLageKorrektKjonnBasertPaaFodselsnummer() {
        String fodselsnummerMann = fodselsnummer;
        String fodselsnummerKvinne = "07063000250"; //TESTFAMILIE

        assertThat(FodselsnummerUtils.lagKjonn(fodselsnummerMann)).isEqualToIgnoringCase("M");
        assertThat(FodselsnummerUtils.lagKjonn(fodselsnummerKvinne)).isEqualToIgnoringCase("K");
    }

    @Test
    public void skalLageFodselsdatoStringPaaUTCFormat() {
        assertThat(FodselsnummerUtils.lagFodselsdato(fodselsdato)).isEqualTo("1980-10-10T00:00:00Z");
    }
}
