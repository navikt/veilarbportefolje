package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus.MANUELL;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.Assertions.assertThat;


class ManuellStatusServiceTest extends EndToEndTest {
    private final OppfolgingRepository oppfolgingRepository;
    private final ManuellStatusService manuellStatusService;
    private final OpensearchTestClient opensearchTestClient;
    private final JdbcTemplate oracle;

    @Autowired
    public ManuellStatusServiceTest(OppfolgingRepository oppfolgingRepository, ManuellStatusService manuellStatusService, OpensearchTestClient opensearchTestClient, JdbcTemplate oracle) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.manuellStatusService = manuellStatusService;
        this.opensearchTestClient = opensearchTestClient;
        this.oracle = oracle;
    }
    @BeforeEach
    public void reset(){
        oracle.update("truncate TABLE OPPFOLGING_DATA");
        oracle.update("truncate TABLE OPPFOLGINGSBRUKER");
        oracle.update("truncate TABLE AKTOERID_TO_PERSONID");
    }

    @Test
    void skal_oppdatere_oversikten_når_bruker_blir_satt_til_manuell() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());

        ManuellStatusDTO melding = new ManuellStatusDTO(aktoerId.toString(), true);
        manuellStatusService.behandleKafkaMeldingLogikk(melding);

        final BrukerOppdatertInformasjon oppfolgingData = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();

        assertThat(oppfolgingData.getManuell()).isTrue();
        pollOpensearchUntil(() -> opensearchTestClient.hentBrukerFraOpensearch(aktoerId).getManuell_bruker().equals(MANUELL.name()));
    }

    @Test
    void skal_oppdatere_oversikten_når_bruker_blir_satt_til_digital_oppfølging() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());

        ManuellStatusDTO melding = new ManuellStatusDTO(aktoerId.toString(), false);
        manuellStatusService.behandleKafkaMeldingLogikk(melding);

        final BrukerOppdatertInformasjon oppfolgingData = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();

        assertThat(oppfolgingData.getManuell()).isFalse();
        pollOpensearchUntil(() -> opensearchTestClient.hentBrukerFraOpensearch(aktoerId).getManuell_bruker() == null);
    }
}
