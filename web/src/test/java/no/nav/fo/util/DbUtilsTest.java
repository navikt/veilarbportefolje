package no.nav.fo.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DbUtilsTest {

    @Rule
    public ExpectedException expectedException =ExpectedException.none();

    @Test
    public void skalReturnereNullHvisDatoenErNull() {
        assertThat(DbUtils.parseDato(null)).isEqualTo(null);
    }

    @Test
    public void skalReturnereNullHvisDatoenErUfullstendig() {
        assertThat(DbUtils.parseDato("TZ")).isEqualTo(null);
    }

    @Test
    public void skalReturnereDatostrengenHvisDatoenErOk() {
        Object dato = "2017-01-01'T'23:23:23.23'Z'";
        assertThat(DbUtils.parseDato(dato)).isEqualTo(dato);
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

    @Test
    public void skalIkkeParseUgyldigJaNeiType() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        DbUtils.parseJaNei(new Object(), "test");
    }

    @Test
    public void skalIkkeParseUgyldigJaNeiInput() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        DbUtils.parseJaNei("foo", "test");
    }

    @Test
    public void kapitaliserTaklerUlikeKombinasjoner() {
        assertThat(DbUtils.kapitaliser("navn")).isEqualTo("Navn");
        assertThat(DbUtils.kapitaliser("Navn")).isEqualTo("Navn");
        assertThat(DbUtils.kapitaliser("NAVN")).isEqualTo("Navn");
        assertThat(DbUtils.kapitaliser("nAVN")).isEqualTo("Navn");
        assertThat(DbUtils.kapitaliser("navn NAVNESEN")).isEqualTo("Navn Navnesen");
        assertThat(DbUtils.kapitaliser("æØÅÄÖÈÊËÉÌÎÏÍ")).isEqualTo("Æøåäöèêëéìîïí");
        assertThat(DbUtils.kapitaliser("hei-hei")).isEqualTo("Hei-Hei");
        assertThat(DbUtils.kapitaliser("o'hara")).isEqualTo("O'Hara");
    }

    @Test
    public void kapitaliserTaklerNull() {
        assertThat(DbUtils.kapitaliser(null)).isNull();
    }

}
