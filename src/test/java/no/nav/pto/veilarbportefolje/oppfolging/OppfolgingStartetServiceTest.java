package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class OppfolgingStartetServiceTest extends EndToEndTest {

    private final OppfolgingStartetService oppfolgingStartetService;
    private final OppfolgingRepository oppfolgingRepository;

    @Autowired
    public OppfolgingStartetServiceTest(OppfolgingStartetService oppfolgingStartetService, OppfolgingRepository oppfolgingRepository) {
        this.oppfolgingStartetService = oppfolgingStartetService;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    @Test
    void skal_sette_bruker_under_oppfølging_i_databasen() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        final String payload = new JSONObject()
                .put("aktorId", aktoerId.getValue())
                .put("oppfolgingStartet", "2020-12-01T00:00:00+02:00")
                .toString();

        oppfolgingStartetService.behandleKafkaMelding(payload);

        final BrukerOppdatertInformasjon info = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();
        assertThat(info.getOppfolging()).isTrue();
        assertThat(info.getNyForVeileder()).isFalse();
    }
}
