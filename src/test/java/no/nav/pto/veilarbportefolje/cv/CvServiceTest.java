package no.nav.pto.veilarbportefolje.cv;

import org.joda.time.DateTime;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CvServiceTest {

    @Test
    public void skal_vurdere_cv_som_delt_om_bruker_har_endret_cv_etter_registreringstidspunkt() {
        DateTime sistEndret = DateTime.now();
        Optional<ZonedDateTime> registreringstidspunkt = Optional.of(ZonedDateTime.now().minusDays(1));

        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(sistEndret, registreringstidspunkt);
        assertThat(harDeltCvMedNav).isTrue();
    }

    @Test
    public void skal_ikke_vurdere_cv_som_delt_om_vi_ikke_kan_hente_registreringsdato() {
        DateTime sistEndret = DateTime.now();
        Optional<ZonedDateTime> registreringstidspunkt = Optional.empty();

        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(sistEndret, registreringstidspunkt);
        assertThat(harDeltCvMedNav).isFalse();
    }

    @Test
    public void skal_ikke_vurdere_cv_som_delt_om_bruker_har_endret_cv_før_registreringstidspunkt() {
        DateTime sistEndret = DateTime.now().minusDays(1);
        Optional<ZonedDateTime> registreringstidspunkt = Optional.of(ZonedDateTime.now());

        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(sistEndret, registreringstidspunkt);
        assertThat(harDeltCvMedNav).isFalse();
    }

    @Test
    public void skal_ikke_vurdere_cv_som_delt_om_bruker_har_endre_cv_samtidig_som_registreringstidspunkt() {
        String isoString = "2020-05-05T00:00:00+02:00";
        DateTime sistEndret = DateTime.parse(isoString);
        Optional<ZonedDateTime> registreringstidspunkt = Optional.of(ZonedDateTime.parse(isoString));

        boolean harDeltCvMedNav = CvService.harDeltCvMedNav(sistEndret, registreringstidspunkt);
        assertThat(harDeltCvMedNav).isFalse();

    }

}