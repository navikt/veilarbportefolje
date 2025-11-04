package no.nav.pto.veilarbportefolje.domene;


import io.getunleash.DefaultUnleash;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.domene.frontendmodell.Etiketter;
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SensurerBrukerTest {

    private AuthService authService;
    private AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient;
    private AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient;

    private PoaoTilgangWrapper poaoTilgangWrapper;

    @Before
    public void setUp() {
        DefaultUnleash defaultUnleash = mock(DefaultUnleash.class);

        poaoTilgangWrapper = mock(PoaoTilgangWrapper.class);
        authService = new AuthService(
                azureAdOnBehalfOfTokenClient,
                azureAdMachineToMachineTokenClient,
                poaoTilgangWrapper
        );
    }

    @Test
    public void skalIkkeSeKode6Bruker() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker());
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere);
    }

    @Test
    public void skalIkkeSeKode7Bruker() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker());
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere);
    }

    @Test
    public void skalIkkeSeEgenAnsatt() {
        when(poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt());
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere);
    }

    @Test
    public void skalSeKode6Bruker() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Permit.INSTANCE);
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker());
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnKode6");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvKode6");
    }

    @Test
    public void skalSeKode7Bruker() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(Decision.Permit.INSTANCE);
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker());
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnKode7");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvKode7");
    }

    @Test
    public void skalSeEgenAnsatt() {
        when(poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt()).thenReturn(Decision.Permit.INSTANCE);
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt());
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnKodeEgenAnsatt");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvEgenAnsatt");
    }

    @Test
    public void skalSeIkkeKonfidensiellBruker() {
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(ikkeKonfidensiellBruker());
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnIkkeKonfidensiellBruker");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvIkkeKonfidensiellBruker");
    }

    @Test
    public void skalIkkeSeKode6Barn() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode6Barn());
        sjekkAtBarnMedKode6ErFjernet(filtrerteBrukere);
    }

    @Test
    public void skalIkkeSeKode7Barn() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode7Barn());
        sjekkAtBarnMedKode7ErFjernet(filtrerteBrukere);
    }

    @Test
    public void skalFjerneKode7BarnMenIkkeKode6() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilKode7()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrertBruker = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode6og7Barn());
        sjekkAtBarnMedKode7ErFjernet(filtrertBruker);
        sjekkAtBarnMedKode6IkkeErFjernet(filtrertBruker);
        assertEquals(2, filtrertBruker.getBarnUnder18AarData().size());
    }

    @Test
    public void skalIkkeSeKode19Barn() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(new Decision.Deny("", ""));
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode19Barn());
        sjekkAtBarnMedKode19ErFjernet(filtrerteBrukere);
    }

    @Test
    public void skalSeKode19Barn() {
        when(poaoTilgangWrapper.harVeilederTilgangTilKode6()).thenReturn(Decision.Permit.INSTANCE);
        PortefoljebrukerFrontendModell filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(brukerMedKode19Barn());
        sjekkAtBarnMedKode19ErIkkeFjernet(filtrerteBrukere);
    }


    private void sjekkAtKonfidensiellDataErVasket(PortefoljebrukerFrontendModell bruker) {
        assertThat(bruker.getFnr()).isEqualTo("");
        assertThat(bruker.getEtternavn()).isEqualTo("");
        assertThat(bruker.getFornavn()).isEqualTo("");
        assertThat(bruker.getSkjermetTil()).isNull();
        assertThat(bruker.getFoedeland()).isNull();
        assertThat(bruker.getTolkebehov().talespraaktolk()).isEqualTo("");
        assertThat(bruker.getTolkebehov().tegnspraaktolk()).isEqualTo("");
        assertThat(bruker.getHovedStatsborgerskap()).isNull();
        assertThat(bruker.getBostedBydel()).isNull();
        assertThat(bruker.getBostedKommune()).isNull();
        assertThat(bruker.getHarUtelandsAddresse()).isEqualTo(false);
    }

    private void sjekkAtBarnMedKode6ErFjernet(PortefoljebrukerFrontendModell bruker) {
        assertTrue(bruker.getBarnUnder18AarData().stream().noneMatch(
                barnUnder18AarData ->
                        barnUnder18AarData.getDiskresjonskode().equals("6")));
    }

    private void sjekkAtBarnMedKode7ErFjernet(PortefoljebrukerFrontendModell bruker) {
        assertTrue(bruker.getBarnUnder18AarData().stream().filter(x -> x.getDiskresjonskode() != null).noneMatch(
                barnUnder18AarData ->
                        barnUnder18AarData.getDiskresjonskode().equals("7")));
    }

    private void sjekkAtBarnMedKode6IkkeErFjernet(PortefoljebrukerFrontendModell bruker) {
        assertTrue(bruker.getBarnUnder18AarData().stream().filter(x -> x.getDiskresjonskode() != null).anyMatch(
                barnUnder18AarData ->
                        barnUnder18AarData.getDiskresjonskode().equals("6")));
    }

    private void sjekkAtBarnMedKode19ErFjernet(PortefoljebrukerFrontendModell bruker) {
        assertTrue(bruker.getBarnUnder18AarData().stream().filter(x -> x.getDiskresjonskode() != null).noneMatch(
                barnUnder18AarData ->
                        barnUnder18AarData.getDiskresjonskode().equals("19")));
    }

    private void sjekkAtBarnMedKode19ErIkkeFjernet(PortefoljebrukerFrontendModell bruker) {
        assertTrue(bruker.getBarnUnder18AarData().stream().filter(x -> x.getDiskresjonskode() != null).anyMatch(
                barnUnder18AarData ->
                        barnUnder18AarData.getDiskresjonskode().equals("19")));
    }

    private PortefoljebrukerFrontendModell kode6Bruker() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setFornavn("fornavnKode6");
        bruker.setEtternavn("etternanvKode6");
        bruker.setDiskresjonskode("6");
        bruker.setBarnUnder18AarData(Collections.emptyList());
        return bruker;
    }

    private PortefoljebrukerFrontendModell kode7Bruker() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setFornavn("fornavnKode7");
        bruker.setEtternavn("etternanvKode7");
        bruker.setDiskresjonskode("7");
        bruker.setBarnUnder18AarData(Collections.emptyList());
        return bruker;
    }

    private PortefoljebrukerFrontendModell egenAnsatt() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setFornavn("fornavnKodeEgenAnsatt");
        bruker.setEtternavn("etternanvEgenAnsatt");
        bruker.setEgenAnsatt(true);
        return bruker;
    }

    private PortefoljebrukerFrontendModell ikkeKonfidensiellBruker() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setFornavn("fornavnIkkeKonfidensiellBruker");
        bruker.setEtternavn("etternanvIkkeKonfidensiellBruker");
        return bruker;
    }

    private PortefoljebrukerFrontendModell brukerMedKode6Barn() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setBarnUnder18AarData(List.of(
                new BarnUnder18AarData(15, "6"),
                new BarnUnder18AarData(12, "6")
        ));
        return bruker;
    }

    private PortefoljebrukerFrontendModell brukerMedKode7Barn() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setBarnUnder18AarData(List.of(
                new BarnUnder18AarData(1, "7")
        ));
        return bruker;
    }

    private PortefoljebrukerFrontendModell brukerMedKode19Barn() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setBarnUnder18AarData(List.of(
                new BarnUnder18AarData(15, "19"),
                new BarnUnder18AarData(12, null),
                new BarnUnder18AarData(3, null)
        ));
        return bruker;
    }

    private PortefoljebrukerFrontendModell brukerMedKode6og7Barn() {
        PortefoljebrukerFrontendModell bruker = frontendbruker;
        bruker.setFnr("11111111111");
        bruker.setBarnUnder18AarData(List.of(
                new BarnUnder18AarData(11, "6"),
                new BarnUnder18AarData(15, "7"),
                new BarnUnder18AarData(3, null)
        ));
        return bruker;
    }

    PortefoljebrukerFrontendModell frontendbruker = new PortefoljebrukerFrontendModell(
            new Etiketter(),
            false,
            false,
            false,
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.emptyMap(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null
    );

}
