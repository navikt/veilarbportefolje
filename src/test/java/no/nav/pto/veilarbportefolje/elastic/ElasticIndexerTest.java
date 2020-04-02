package no.nav.pto.veilarbportefolje.elastic;

import org.junit.Test;

import static no.nav.pto.veilarbportefolje.elastic.ElasticIndexer.BATCH_SIZE;
import static no.nav.pto.veilarbportefolje.elastic.ElasticIndexer.til;
import static org.assertj.core.api.Assertions.*;

public class ElasticIndexerTest {

    @Test
    public void skal_beregne_riktig_page_size_naar_fra_er_0() {
        assertThat(til(0, BATCH_SIZE)).isEqualTo(BATCH_SIZE);
    }

    @Test
    public void skal_beregne_riktig_page_size() {
        assertThat(til(3000, BATCH_SIZE)).isEqualTo(3000 + BATCH_SIZE);
    }

    @Test
    public void skal_kaste_exception_om_page_size_faar_negativ_input() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> til(-1, BATCH_SIZE));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> til(1, -1000));
    }
}
