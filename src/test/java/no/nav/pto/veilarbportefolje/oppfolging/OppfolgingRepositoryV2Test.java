package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OppfolgingRepositoryV2Test {
    private OppfolgingRepositoryV2 oppfolgingRepository;
    private final AktorId aktoerId = TestDataUtils.randomAktorId();

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        db.execute("truncate oppfolging_data");
        oppfolgingRepository = new OppfolgingRepositoryV2(db);
    }

    @Test
    public void skal_ut_av_oppfolging() {
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.slettOppfolgingData(aktoerId);

        List<AktorId> aktorIds = oppfolgingRepository.hentAlleBrukereUnderOppfolging();
        assertThat(aktorIds.isEmpty()).isTrue();
    }

    @Test
    public void skal_sette_ny_veileder() {
        VeilederId veilederId = VeilederId.of("Z12345");
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settVeileder(aktoerId, veilederId);

        BrukerOppdatertInformasjon brukerOppdatertInformasjon = oppfolgingRepository.hentOppfolgingData(aktoerId).get();
        List<AktorId> aktorIds = oppfolgingRepository.hentAlleBrukereUnderOppfolging();

        assertThat(aktorIds.isEmpty()).isFalse();
        assertThat(aktorIds.get(0)).isEqualTo(aktoerId);
        assertThat(VeilederId.of(brukerOppdatertInformasjon.getVeileder())).isEqualTo(veilederId);
    }

}
