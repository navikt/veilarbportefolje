package no.nav.pto.veilarbportefolje.util;

import org.junit.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class FodselsnummerUtilsTest {
    private final LocalDate fodselsdato = LocalDate.of(1980, 10, 10);

    @Test
    public void skalLageFodselsdatoStringPaaUTCFormat() {
        assertThat(FodselsnummerUtils.lagFodselsdato(fodselsdato)).isEqualTo("1980-10-10T00:00:00Z");
    }
}
