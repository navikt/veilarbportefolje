package no.nav.pto.veilarbportefolje.controller;

import no.nav.common.abac.Pep;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;


import static no.nav.common.auth.subject.IdentType.InternBruker;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnhetControllerTest {

    private ElasticService elasticService;
    private EnhetController enhetController;
    private Pep pep;

    @Before
    public void initController() {
        elasticService = mock(ElasticService.class);
        pep = mock(Pep.class);
        AuthService authService = new AuthService(pep);
        enhetController = new EnhetController(elasticService, authService, mock(MetricsClient.class), mock(TiltakService.class));

    }

    @Test
    public void skal_hent_portefolje_fra_indeks_dersom_tilgang() {
        when(pep.harVeilederTilgangTilModia(anyString())).thenReturn(true);
        when(pep.harVeilederTilgangTilEnhet(anyString(), anyString())).thenReturn(true);
        when(elasticService.hentBrukere(any(), any(), any(), any() , any(), any(), any())).thenReturn(new BrukereMedAntall(0, Collections.emptyList()));

        SubjectHandler.withSubject(
                new Subject("testident", InternBruker, SsoToken.oidcToken("token", Collections.emptyMap())),
                () -> enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", "ikke_satt", new Filtervalg()));
        verify(elasticService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skal_hente_hele_portefolje_fra_indeks_dersom_man_mangle_antall() {
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilModia(any())).thenReturn(true);
        when(elasticService.hentBrukere(any(), any(), any(), any() , any(), any(), any())).thenReturn(new BrukereMedAntall(0, Collections.emptyList()));

        SubjectHandler.withSubject(
                new Subject("testident", InternBruker, SsoToken.oidcToken("token", Collections.emptyMap())),
                () -> enhetController.hentPortefoljeForEnhet("0001", 0, null, "ikke_satt", "ikke_satt", new Filtervalg()));
        verify(elasticService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skal_hente_hele_portefolje_fra_indeks_dersom_man_mangle_fra() {
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilModia(any())).thenReturn(true);
        when(elasticService.hentBrukere(any(), any(), any(), any() , any(), any(), any())).thenReturn(new BrukereMedAntall(0, Collections.emptyList()));

        SubjectHandler.withSubject(
                new Subject("testident", InternBruker, SsoToken.oidcToken("token", Collections.emptyMap())),
                () -> enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", "ikke_satt", new Filtervalg()));


        verify(elasticService, times(1)).hentBrukere(any(), any(), any(), any(), any(), isNull(), any());
    }

    @Test(expected = ResponseStatusException.class)
    public void skal_ikke_hente_noe_hvis_mangler_tilgang() {
        when(pep.harVeilederTilgangTilModia(any())).thenReturn(false);

        SubjectHandler.withSubject(
                new Subject("testident", InternBruker, SsoToken.oidcToken("token", Collections.emptyMap())),
                () -> enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", "ikke_satt", new Filtervalg()));
    }
}
