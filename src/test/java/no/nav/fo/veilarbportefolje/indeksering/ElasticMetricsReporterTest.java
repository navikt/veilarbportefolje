package no.nav.fo.veilarbportefolje.indeksering;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ElasticMetricsReporterTest {

    @Test
    void skal_returnere_true_naar_over_26_timer_gammel() {
        String indeksNavn = "brukerindeks_p_20191008_0416";
        boolean result = ElasticMetricsReporter.erOver26TimerGammel(indeksNavn);
        assertThat(result).isTrue();
    }

    @Test
    void skal_returnere_false_naar_under_26_timer_gammel() {
        String indeksNavn = ElasticUtils.createIndexName("brukerindeks_p");
        boolean result = ElasticMetricsReporter.erOver26TimerGammel(indeksNavn);
        assertThat(result).isFalse();
    }
}