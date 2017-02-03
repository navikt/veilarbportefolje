package no.nav.fo.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DbUtilsTest {

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
}