package no.nav.pto.veilarbportefolje.huskelapp;

import no.nav.common.abac.VeilarbPep;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.HuskelappController;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static no.nav.common.json.JsonUtils.toJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HuskelappController.class)
@Import(ApplicationConfigTest.class)
public class HuskelappControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    HuskelappRepository huskelappRepository;

    @MockBean
    private AuthService authService;

    @MockBean
    private PoaoTilgangWrapper poaoTilgangWrapper;

    @MockBean
    private VeilarbPep veilarbPep;

    @MockBean
    private BrukerServiceV2 brukerService;

    @MockBean
    private AktorClient aktorClient;

    @Test
    void test_at_vi_faar_serialisert_record_HuskelappForBrukerRequest() throws Exception {
        Fnr fnr = Fnr.of("12345678910");
		EnhetId enhetId = EnhetId.of("1234");
        AktorId aktorId = AktorId.of("1223234234234");
		HuskelappOpprettRequest request = new HuskelappOpprettRequest(fnr, null, "Test", enhetId);
		UUID opprettetUUID = UUID.randomUUID();


        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harTilgangTilPerson(any())).thenReturn(Decision.Permit.INSTANCE);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(veilarbPep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of("1234")));
        when(aktorClient.hentAktorId(any())).thenReturn(aktorId);
		when(brukerService.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(AuthUtils.getInnloggetVeilederIdent()));
		when(huskelappRepository.opprettHuskelapp(eq(request), any())).thenReturn(opprettetUUID);


        mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(request))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(201))
                .andExpect(content()
                .string(toJson(opprettetUUID)));
    }
}
