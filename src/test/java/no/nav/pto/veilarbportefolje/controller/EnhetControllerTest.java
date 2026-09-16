package no.nav.pto.veilarbportefolje.controller;

import lombok.SneakyThrows;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.aktiviteter.v1.TiltaksaktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt;
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.persononinfo.bosted.BostedService;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.PersonOpprinnelseService;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static no.nav.pto.veilarbportefolje.domene.FiltervalgDefaultsKt.getFiltervalgDefaults;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class EnhetControllerTest {

    private OpensearchService opensearchService;
    private EnhetController enhetController;
    private PoaoTilgangWrapper poaoTilgangWrapper;
    private AuthContextHolder authContextHolder;

    Filtervalg filtervalgDefaults = getFiltervalgDefaults();

    @BeforeEach
    public void initController() {
        opensearchService = mock(OpensearchService.class);
        poaoTilgangWrapper = mock(PoaoTilgangWrapper.class);
        authContextHolder = AuthContextHolderThreadLocal.instance();

        AuthService authService = new AuthService(
                mock(AzureAdOnBehalfOfTokenClient.class),
                mock(AzureAdMachineToMachineTokenClient.class),
                poaoTilgangWrapper
        );
        enhetController = new EnhetController(opensearchService, authService, mock(TiltakService.class), mock(PersonOpprinnelseService.class), mock(BostedService.class), mock(TiltaksaktivitetService.class));
    }

    @Test
    @SneakyThrows
    public void skal_hent_portefolje_fra_indeks_dersom_tilgang() {
        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilEnhet(any())).thenReturn(Decision.Permit.INSTANCE);
        when(opensearchService.hentBrukere(any(), any(), any(), any(), any(), any(), any())).thenReturn(new BrukereMedAntall(0, Collections.emptyList()));

        authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, TestDataUtils.generateJWT("A111111")),
                () -> enhetController.hentPortefoljeForEnhet("0001", 0, 0, "ikke_satt", Sorteringsfelt.IKKE_SATT.sorteringsverdi, filtervalgDefaults)
        );
        verify(opensearchService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skal_hente_hele_portefolje_fra_indeks_dersom_man_mangle_antall() {
        when(poaoTilgangWrapper.harVeilederTilgangTilEnhet(any())).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(opensearchService.hentBrukere(any(), any(), any(), any(), any(), any(), any())).thenReturn(new BrukereMedAntall(0, Collections.emptyList()));

        authContextHolder.withContext(
                new AuthContext(UserRole.INTERN, TestDataUtils.generateJWT("A111111")),
                () -> enhetController.hentPortefoljeForEnhet("0001", 0, null, "ikke_satt", Sorteringsfelt.IKKE_SATT.sorteringsverdi, filtervalgDefaults)
        );
        verify(opensearchService, times(1)).hentBrukere(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void skal_hente_hele_portefolje_fra_indeks_dersom_man_mangle_fra() {
        when(poaoTilgangWrapper.harVeilederTilgangTilEnhet(any())).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(opensearchService.hentBrukere(any(), any(), any(), any(), any(), any(), any())).thenReturn(new BrukereMedAntall(0, Collections.emptyList()));
        authContextHolder
                .withContext(
                        new AuthContext(UserRole.INTERN, TestDataUtils.generateJWT("A111111")),
                        () -> enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", Sorteringsfelt.IKKE_SATT.sorteringsverdi, filtervalgDefaults)
                );

        verify(opensearchService, times(1)).hentBrukere(any(), any(), any(), any(), any(), isNull(), any());
    }

    @Test
    public void skal_ikke_hente_noe_hvis_mangler_tilgang() {
        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(new Decision.Deny("", ""));

        assertThrows(ResponseStatusException.class, () -> {
            authContextHolder.withContext(
                    new AuthContext(UserRole.INTERN, TestDataUtils.generateJWT("A111111")),
                    () -> enhetController.hentPortefoljeForEnhet("0001", null, 20, "ikke_satt", Sorteringsfelt.IKKE_SATT.sorteringsverdi, filtervalgDefaults)
            );
        });
    }
}
