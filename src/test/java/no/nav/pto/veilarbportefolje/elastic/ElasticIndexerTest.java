package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.pto.veilarbportefolje.elastic.ElasticIndexer.BATCH_SIZE;
import static no.nav.pto.veilarbportefolje.elastic.ElasticIndexer.utregnTil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticIndexerTest {

    @InjectMocks
    private ElasticIndexer elasticIndexer;

    @Mock
    private CvService cvService;

    @Mock
    private UnleashService unleashService;

    @Test
    public void skal_spole_tilbake_kafka_cv_ved_hovedindeksering() {
        when(unleashService.isEnabled(anyString())).thenReturn(false);
        elasticIndexer.startIndeksering(true);
        Mockito.verify(cvService, Mockito.times(1)).setRewind(anyBoolean());
    }

    @Test
    public void skal_beregne_riktig_page_size_naar_fra_er_0() {
        assertThat(utregnTil(0, BATCH_SIZE)).isEqualTo(BATCH_SIZE);
    }

    @Test
    public void skal_beregne_riktig_page_size() {
        assertThat(utregnTil(3000, BATCH_SIZE)).isEqualTo(3000 + BATCH_SIZE);
    }

    @Test
    public void skal_kaste_exception_om_page_size_faar_negativ_input() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> utregnTil(-1, BATCH_SIZE));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> utregnTil(1, -1000));
    }
}
