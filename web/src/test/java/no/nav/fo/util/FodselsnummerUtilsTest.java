package no.nav.fo.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FodselsnummerUtilsTest {
    private String fodselsnummer = "***REMOVED***";
    private String dnummer = "***REMOVED***";

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
        String fodselsnummerKvinne = "***REMOVED***";

        assertThat(FodselsnummerUtils.lagKjonn(fodselsnummerMann)).isEqualToIgnoringCase("M");
        assertThat(FodselsnummerUtils.lagKjonn(fodselsnummerKvinne)).isEqualToIgnoringCase("K");
    }

    @Test
    public void skalLageFodselsdagIMndFraForsteNummerIFodselsnummerOgHandtereDNummer() {
        String fodselsnummer2 = "***REMOVED***";
        String fodselsnummer3 = "***REMOVED***";
        String fodselsnummer4 = "***REMOVED***";
        String dnummer1 = "***REMOVED***";
        String dnummer2 = "***REMOVED***";
        String dnummer3 = "***REMOVED***";

        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer)).isEqualTo(18);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer)).isEqualTo(18);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer2)).isEqualTo(28);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer3)).isEqualTo(30);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(fodselsnummer4)).isEqualTo(1);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer1)).isEqualTo(18);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer2)).isEqualTo(21);
        assertThat(FodselsnummerUtils.lagFodselsdagIMnd(dnummer3)).isEqualTo(31);
    }

    @Test
    public void skalLageFodselsdatoStringPaaUTCFormat() {
        assertThat(FodselsnummerUtils.lagFodselsdato(fodselsnummer)).isEqualTo("1991-12-18T00:00:00Z");
        assertThat(FodselsnummerUtils.lagFodselsdato(dnummer)).isEqualTo("1991-12-18T00:00:00Z");
    }

    @Test
    public void skalLageAarstallFraFodselsnummer() {
        String fodselsnummer1900a = "***REMOVED***";
        String fodselsnummer1900b = "***REMOVED***";
        String fodselsnummer2000a = "***REMOVED***";
        String fodselsnummer2000b = "***REMOVED***";

        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer)).isEqualTo(1991);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(dnummer)).isEqualTo(1991);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer1900a)).isEqualTo(1918);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer1900b)).isEqualTo(1902);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer2000a)).isEqualTo(2018);
        assertThat(FodselsnummerUtils.lagAarstallFraFodselsnummer(fodselsnummer2000b)).isEqualTo(2002);

    }
}