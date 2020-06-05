package no.nav.pto.veilarbportefolje.cv;

import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class CvServiceTest {

    @Test
    public void skal_vurdere_cv_som_delt_om_bruker_har_endret_cv_etter_oppfølging_startet() {
        Instant oppfolgingStartet = Instant.now().minusSeconds(60);
        Instant cvSistEndret = Instant.now();
        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(oppfolgingStartet, cvSistEndret);
        assertThat(harDeltCvMedNav).isTrue();
    }

    @Test
    public void skal_ikke_vurdere_cv_som_delt_om_bruker_har_endret_cv_før_oppfølging_startet() {
        Instant oppfolgingStartet = Instant.now();
        Instant cvSistEndret = Instant.now().minusSeconds(60);
        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(oppfolgingStartet, cvSistEndret);
        assertThat(harDeltCvMedNav).isFalse();
    }

    @Test
    public void skal_ikke_vurdere_cv_som_delt_om_bruker_har_endret_cv_samtidig_som_registreringstidspunkt() {
        Instant now = Instant.now();
        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(now, now);
        assertThat(harDeltCvMedNav).isFalse();

    }
}