package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OppfolgingStartetServiceTest extends EndToEndTest {
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OppfolgingPeriodeService oppfolgingPeriodeService;

    @Autowired
    public OppfolgingStartetServiceTest(OppfolgingRepositoryV2 oppfolgingRepositoryV2, OppfolgingAvsluttetService oppfolgingAvsluttetService) {
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        AktorClient aktorClient = mock(AktorClient.class);
        when(aktorClient.hentFnr(any())).thenReturn(Fnr.of("-1"));
        OppfolgingStartetService oppfolgingStartetService = new OppfolgingStartetService(oppfolgingRepositoryV2, mock(OpensearchIndexer.class), mock(PdlService.class));
        this.oppfolgingPeriodeService = new OppfolgingPeriodeService(oppfolgingStartetService, oppfolgingAvsluttetService);
    }

    @Test
    public void skal_sette_bruker_under_oppfølging_i_databasen() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();

        SisteOppfolgingsperiodeV1 payload = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse("2020-12-01T00:00:00+02:00"), null);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(payload);

        final BrukerOppdatertInformasjon info = oppfolgingRepositoryV2.hentOppfolgingData(aktoerId).orElseThrow();
        assertThat(info.getOppfolging()).isTrue();
        assertThat(info.getNyForVeileder()).isFalse();
    }
}
