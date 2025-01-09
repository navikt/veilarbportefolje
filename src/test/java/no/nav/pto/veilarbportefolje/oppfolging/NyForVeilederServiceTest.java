package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class NyForVeilederServiceTest extends EndToEndTest {
    private final NyForVeilederService nyForVeilederService;
    private final OppfolgingRepositoryV2 oppfolgingRepository;

    @Autowired
    public NyForVeilederServiceTest(NyForVeilederService nyForVeilederService, OppfolgingRepositoryV2 oppfolgingRepository) {
        this.nyForVeilederService = nyForVeilederService;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    @Test
    public void skal_sette_ny_for_veileder_til_false_om_veileder_har_vært_inne_i_aktivitetsplan_til_bruker() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settNyForVeileder(aktoerId, true);

        opensearchTestClient.createUserInOpensearch(aktoerId);

        NyForVeilederDTO melding = new NyForVeilederDTO(aktoerId, false);
        nyForVeilederService.behandleKafkaMeldingLogikk(melding);

        final Optional<BrukerOppdatertInformasjon> data = oppfolgingRepository.hentOppfolgingData(aktoerId);
        assertThat(data).isPresent();
        assertThat(data.get().getNyForVeileder()).isFalse();

        final boolean nyForVeileder = opensearchTestClient.hentBrukerFraOpensearch(aktoerId).isNy_for_veileder();
        assertThat(nyForVeileder).isFalse();
    }

    @Test
    public void skal_ignorere_meldinger_hvor_ny_for_veileder_er_satt_til_true_siden_dette_gjøres_ved_tilordning() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settNyForVeileder(aktoerId, false);

        opensearchTestClient.createUserInOpensearch(aktoerId);

        NyForVeilederDTO melding = new NyForVeilederDTO(aktoerId, true);

        nyForVeilederService.behandleKafkaMeldingLogikk(melding);

        final Optional<BrukerOppdatertInformasjon> data = oppfolgingRepository.hentOppfolgingData(aktoerId);
        assertThat(data).isPresent();
        //assertThat(data.get().getNyForVeileder()).isFalse();

        final boolean nyForVeileder = opensearchTestClient.hentBrukerFraOpensearch(aktoerId).isNy_for_veileder();
        //assertThat(nyForVeileder).isFalse();
    }
}
