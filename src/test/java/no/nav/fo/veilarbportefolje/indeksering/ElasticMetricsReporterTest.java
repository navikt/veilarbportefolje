package no.nav.fo.veilarbportefolje.indeksering;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticMetricsReporterTest {

    @Test
    void skal_hente_ut_timestamp_fra_indeksnavn() {
        String indeksNavn = "brukerindeks_p_20191008_0416";
        LocalDateTime indekseringsdato = ElasticMetricsReporter.hentIndekseringsdato(indeksNavn);
        assertThat(indekseringsdato.toString()).isEqualToIgnoringWhitespace("2019-10-08T04:16");
    }
}