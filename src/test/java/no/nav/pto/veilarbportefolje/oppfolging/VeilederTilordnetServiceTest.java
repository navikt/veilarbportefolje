package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

class VeilederTilordnetServiceTest extends EndToEndTest {

    @Autowired
    private final AktorClient aktorClient;
    private final VeilederTilordnetService veilederTilordnetService;

    @Autowired
    public VeilederTilordnetServiceTest(AktorClient aktorClient, VeilederTilordnetService veilederTilordnetService) {
        this.aktorClient = aktorClient;
        this.veilederTilordnetService = veilederTilordnetService;
    }

    @Test
    public void skal_oppdatere_tilordnet_veileder() {
        final AktorId aktoerId = randomAktorId();
        final VeilederId nyVeileder = randomVeilederId();
        final ZonedDateTime tilordnet = ZonedDateTime.now();
        when(aktorClient.hentFnr(aktoerId)).thenReturn(randomFnr());

        testDataClient.lagreBrukerUnderOppfolging(aktoerId, randomNavKontor(), randomVeilederId(), ZonedDateTime.now(), null);

        veilederTilordnetService.behandleKafkaMeldingLogikk(new VeilederTilordnetDTO(aktoerId, nyVeileder, tilordnet));

        final OppfolgingsBruker bruker = opensearchTestClient.hentBrukerFraOpensearch(aktoerId);
        final VeilederId tilordnetVeileder = VeilederId.of(bruker.getVeileder_id());


        assertThat(tilordnetVeileder).isEqualTo(nyVeileder);
        assertThat(bruker.isNy_for_veileder()).isTrue();
    }

    @Test
    public void skal_oppdatere_tilordnet_veileder_med_null() {
        final AktorId aktoerId = randomAktorId();
        final VeilederId nyVeileder = VeilederId.of(null);
        when(aktorClient.hentFnr(aktoerId)).thenReturn(randomFnr());

        testDataClient.lagreBrukerUnderOppfolging(aktoerId, randomNavKontor(), randomVeilederId(), ZonedDateTime.now(), null);

        veilederTilordnetService.behandleKafkaMeldingLogikk(new VeilederTilordnetDTO(aktoerId, nyVeileder, null));

        final OppfolgingsBruker bruker = opensearchTestClient.hentBrukerFraOpensearch(aktoerId);
        final VeilederId tilordnetVeileder = VeilederId.of(bruker.getVeileder_id());


        assertThat(tilordnetVeileder.getValue()).isNull();
        assertThat(bruker.isNy_for_veileder()).isTrue();
    }
}
