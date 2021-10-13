package no.nav.pto.veilarbportefolje.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DbUtilsTest {

    @Test
    public void skal_konvertere_boolean_til_ja_nei() {
        String ja = DbUtils.boolToJaNei(true);
        assertThat(ja).isEqualTo("J");

        String nei = DbUtils.boolToJaNei(false);
        assertThat(nei).isEqualTo("N");
    }

    @Test
    public void skalParseJaNei() throws Exception {
        boolean shouldBeTrue = DbUtils.parseJaNei("J", "test");
        boolean shouldBeFalse = DbUtils.parseJaNei("N", "test");
        assertThat(shouldBeTrue).isTrue();
        assertThat(shouldBeFalse).isFalse();
    }

    @Test
    public void skalTakleNull() throws Exception {
        boolean shouldBeFalse = DbUtils.parseJaNei(null, "test");
        assertThat(shouldBeFalse).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void skalIkkeParseUgyldigJaNeiType() {
        DbUtils.parseJaNei(new Object(), "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void skalIkkeParseUgyldigJaNeiInput() throws Exception {
        DbUtils.parseJaNei("foo", "test");
    }
}
