package no.nav.fo.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FodselsnummerUtilsTest {
    private String fodselsnummer = "10108000399"; //TESTFAMILIE
    private String dnummer = "50108000399"; //TESTFAMILIE

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
    public void skalLageFodselsdagIMndFraForsteNummerIFodselsnummerOgHandtereDNummer() {
        String fodselsnummer2 = "10108000397"; //TESTFAMILIE
        String fodselsnummer3 = "10108000398"; //TESTFAMILIE
        String fodselsnummer4 = "10108000399"; //TESTFAMILIE
        String dnummer1 = "50108000397"; //TESTFAMILIE
        String dnummer2 = "50108000398"; //TESTFAMILIE
        String dnummer3 = "50108000399"; //TESTFAMILIE

        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer2)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer3)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer4)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer1)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer2)).isEqualTo("10");
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer3)).isEqualTo("10");
    }

    @Test
    public void skalLageFodselsdatoStringPaaUTCFormat() {
        assertThat(FodselsnummerUtils.lagFodselsdato(fodselsnummer)).isEqualTo("1980-10-10T00:00:00Z");
        assertThat(FodselsnummerUtils.lagFodselsdato(dnummer)).isEqualTo("1980-10-10T00:00:00Z");
    }

    @Test
    public void skalLageAarstallFraFodselsnummer() {
        String fodselsnummer1900a = "00001849900";
        String fodselsnummer1900b = "00000249900";
        String fodselsnummer2000a = "00001850000";
        String fodselsnummer2000b = "00000250000";

        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer)).isEqualTo(1980);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(dnummer)).isEqualTo(1980);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer1900a)).isEqualTo(1918);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer1900b)).isEqualTo(1902);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer2000a)).isEqualTo(2018);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer2000b)).isEqualTo(2002);

    }
}