package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus.MANUELL;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.Assertions.assertThat;


class ManuellStatusServiceTest extends EndToEndTest {
    private final OppfolgingRepository oppfolgingRepository;
    private final ManuellStatusService manuellStatusService;
    private final OpensearchTestClient opensearchTestClient;

    @Autowired
    public ManuellStatusServiceTest(OppfolgingRepository oppfolgingRepository, ManuellStatusService manuellStatusService, OpensearchTestClient opensearchTestClient) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.manuellStatusService = manuellStatusService;
        this.opensearchTestClient = opensearchTestClient;
    }

    @Test
    void skal_oppdatere_oversikten_når_bruker_blir_satt_til_manuell() {
        final AktorId aktoerId = randomAktorId();
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        populateOpensearch(EnhetId.of("0000"), VeilederId.of(null), aktoerId.get());
        opensearchTestClient.createUserInOpensearch(aktoerId);

        ManuellStatusDTO melding = new ManuellStatusDTO(aktoerId.toString(), true);
        manuellStatusService.behandleKafkaMeldingLogikk(melding);

        final BrukerOppdatertInformasjon oppfolgingData = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();

        assertThat(oppfolgingData.getManuell()).isTrue();
        pollOpensearchUntil(() -> opensearchTestClient.hentBrukerFraOpensearch(aktoerId).getManuell_bruker().equals(MANUELL.name()));
    }

    @Test
    void skal_oppdatere_oversikten_når_bruker_blir_satt_til_digital_oppfølging() {
        final AktorId aktoerId = randomAktorId();
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        populateOpensearch(EnhetId.of("0000"), VeilederId.of(null), aktoerId.get());

        ManuellStatusDTO melding = new ManuellStatusDTO(aktoerId.toString(), false);
        manuellStatusService.behandleKafkaMeldingLogikk(melding);

        final BrukerOppdatertInformasjon oppfolgingData = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();

        assertThat(oppfolgingData.getManuell()).isFalse();
        pollOpensearchUntil(() -> opensearchTestClient.hentBrukerFraOpensearch(aktoerId).getManuell_bruker() == null);
    }
}
