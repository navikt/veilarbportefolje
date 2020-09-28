package no.nav.pto.veilarbportefolje.elastic;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsReporterTest {

    @Test
    void skal_hente_ut_timestamp_fra_indeksnavn() {
        String indeksNavn = "brukerindeks_p_20191008_0416";
        LocalDateTime indekseringsdato = MetricsReporter.hentIndekseringsdato(indeksNavn);
        assertThat(indekseringsdato.toString()).isEqualToIgnoringWhitespace("2019-10-08T04:16");
    }
}
