package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.cv.IntegrationTest;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static no.nav.pto.veilarbportefolje.elastic.ElasticIndexer.BATCH_SIZE;
import static no.nav.pto.veilarbportefolje.elastic.ElasticIndexer.utregnTil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;

public class ElasticIndexerTest extends IntegrationTest {

    private static ElasticIndexer elasticIndexer;
    private static CvService cvService;

    @Before
    public void setUp() {
        cvService = mock(CvService.class);
        elasticIndexer = new ElasticIndexer(
                mock(AktivitetDAO.class),
                mock(BrukerRepository.class),
                ELASTIC_CLIENT,
                mock(ElasticService.class),
                mock(UnleashService.class),
                cvService
        );
    }

    @Test
    public void skal_spol_tilbake_kafka_cv_ved_hovedindeksering() {
        elasticIndexer.startIndeksering();
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
