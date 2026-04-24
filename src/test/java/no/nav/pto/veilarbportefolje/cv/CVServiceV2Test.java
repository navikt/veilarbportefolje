package no.nav.pto.veilarbportefolje.cv;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class CVServiceV2Test {

    private CVRepositoryV2 cvRepositoryV2;
    private CVServiceV2 cvServiceV2;

    @Before
    public void setup() {
        OpensearchIndexerPaDatafelt opensearchIndexer = mock(OpensearchIndexerPaDatafelt.class);
        cvRepositoryV2 = mock(CVRepositoryV2.class);
        PdlIdentRepository pdlIdentRepository = mock(PdlIdentRepository.class);
        cvServiceV2 = new CVServiceV2(opensearchIndexer, cvRepositoryV2, pdlIdentRepository);
    }

    @Test
    public void slettCvData_skal_slette_cv_nar_fnr_finnes() {
        AktorId aktorId = AktorId.of("1234567890123");
        Fnr fnr = Fnr.of("12345678901");

        cvServiceV2.slettCvData(aktorId, Optional.of(fnr));

        verify(cvRepositoryV2, times(1)).slettCvRegistrert(fnr);
    }

    @Test
    public void slettCvData_skal_ikke_slette_nar_fnr_er_tom() {
        AktorId aktorId = AktorId.of("1234567890123");

        cvServiceV2.slettCvData(aktorId, Optional.empty());

        verify(cvRepositoryV2, never()).slettCvRegistrert(any());
    }

    @Test
    public void slettCvData_skal_ikke_kaste_exception_ved_feil() {
        AktorId aktorId = AktorId.of("1234567890123");
        Fnr fnr = Fnr.of("12345678901");
        doThrow(new RuntimeException("DB-feil")).when(cvRepositoryV2).slettCvRegistrert(fnr);

        cvServiceV2.slettCvData(aktorId, Optional.of(fnr));

        verify(cvRepositoryV2, times(1)).slettCvRegistrert(fnr);
    }
}
