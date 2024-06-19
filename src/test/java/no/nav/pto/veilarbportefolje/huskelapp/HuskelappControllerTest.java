package no.nav.pto.veilarbportefolje.huskelapp;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.HuskelappController;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.*;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.TestDataClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HuskelappController.class)
@Import(ApplicationConfigTest.class)
public class HuskelappControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected TestDataClient testDataClient;

    @MockBean
    private AuthService authService;

    @MockBean
    private PoaoTilgangWrapper poaoTilgangWrapper;

    @MockBean
    private VeilarbPep veilarbPep;

    @MockBean
    private BrukerServiceV2 brukerService;

    @Autowired
    private PdlIdentRepository pdlIdentRepository;

    @Test
    void test_opprett_og_hent_huskelapp_for_bruker() throws Exception {
        Fnr fnr = Fnr.of("10987654321");
        EnhetId enhetId = EnhetId.of("1234");
        AktorId aktorId = AktorId.of("99988877766655");
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
        LocalDate huskelappfrist = LocalDate.now();
        testDataClient.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId);

        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId));
        when(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));

        HuskelappOpprettRequest opprettRequest = new HuskelappOpprettRequest(fnr, huskelappfrist, "Test", enhetId);
        String opprettetHuskelappId = mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(opprettRequest))
                )
                .andExpect(status().is(201))
                .andReturn().getResponse().getContentAsString();

        HuskelappForBrukerRequest hentForBrukerRequest = new HuskelappForBrukerRequest(fnr, enhetId);
        HuskelappOpprettResponse huskelappId = fromJson(opprettetHuskelappId, HuskelappOpprettResponse.class);
        HuskelappResponse expected = new HuskelappResponse(huskelappId.huskelappId(), fnr, enhetId, huskelappfrist, "Test", LocalDate.now(), veilederId.getValue());

        String hentHuskelappResult = mockMvc
                .perform(
                        post("/api/v1/hent-huskelapp-for-bruker")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(hentForBrukerRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();

        HuskelappResponse hentetHuskelappBody = fromJson(hentHuskelappResult, HuskelappResponse.class);
        assertThat(hentetHuskelappBody.huskelappId()).isEqualTo(expected.huskelappId());
        assertThat(hentetHuskelappBody.brukerFnr()).isEqualTo(expected.brukerFnr());
        assertThat(hentetHuskelappBody.enhetId()).isEqualTo(expected.enhetId());
        assertThat(hentetHuskelappBody.frist()).isEqualTo(expected.frist());
        assertThat(hentetHuskelappBody.kommentar()).isEqualTo(expected.kommentar());
        assertThat(hentetHuskelappBody.endretDato()).isEqualTo(expected.endretDato());
        assertThat(hentetHuskelappBody.endretAv()).isEqualTo(expected.endretAv());

    }

    @Test
    void test_at_vi_redigere_huskelapp() throws Exception {
        Fnr fnr = Fnr.of("12345678910");
        EnhetId enhetId = EnhetId.of("1234");
        AktorId aktorId = AktorId.of("1223234234234");
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
        HuskelappOpprettRequest opprettRequest = new HuskelappOpprettRequest(fnr, LocalDate.now(), "Test", enhetId);
        testDataClient.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId);

        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harTilgangTilPerson(any())).thenReturn(Decision.Permit.INSTANCE);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(veilarbPep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId));
        when(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));

        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        );
        pdlIdentRepository.upsertIdenter(identer);

        String opprettetHuskelappIdbody = mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(opprettRequest))
                ).andReturn().getResponse().getContentAsString();
        HuskelappOpprettResponse huskelappIdBody = fromJson(opprettetHuskelappIdbody, HuskelappOpprettResponse.class);
        HuskelappForBrukerRequest hentForBrukerForRedigeringRequest = new HuskelappForBrukerRequest(fnr, enhetId);
        String hentHuskelappForRedigeringResult = mockMvc
                .perform(
                        post("/api/v1/hent-huskelapp-for-bruker")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(hentForBrukerForRedigeringRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();

        HuskelappResponse hentetHuskelappBody = fromJson(hentHuskelappForRedigeringResult, HuskelappResponse.class);
        assertThat(hentetHuskelappBody.huskelappId()).isEqualTo(huskelappIdBody.huskelappId());
        assertThat(hentetHuskelappBody.frist()).isEqualTo(opprettRequest.frist());
        assertThat(hentetHuskelappBody.kommentar()).isEqualTo(opprettRequest.kommentar());
        assertThat(hentetHuskelappBody.endretAv()).isEqualTo(veilederId.getValue());

        HuskelappRedigerRequest redigereRequest = new HuskelappRedigerRequest(UUID.fromString(huskelappIdBody.huskelappId()), fnr, null, "Test at det blir en ny kommentar", enhetId);
        mockMvc
                .perform(
                        put("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(redigereRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                ).andExpect(status().is(204));

        HuskelappForBrukerRequest hentForBrukerEtterRedigeringRequest = new HuskelappForBrukerRequest(fnr, enhetId);
        String hentHuskelappEtterRedigeringResult = mockMvc
                .perform(
                        post("/api/v1/hent-huskelapp-for-bruker")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(hentForBrukerEtterRedigeringRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();

        HuskelappResponse hentetHuskelappEtterRedigeringBody = fromJson(hentHuskelappEtterRedigeringResult, HuskelappResponse.class);
        assertThat(hentetHuskelappEtterRedigeringBody.huskelappId()).isEqualTo(huskelappIdBody.huskelappId());
        assertThat(hentetHuskelappEtterRedigeringBody.frist()).isEqualTo(redigereRequest.frist());
        assertThat(hentetHuskelappEtterRedigeringBody.kommentar()).isEqualTo(redigereRequest.kommentar());
        assertThat(hentetHuskelappEtterRedigeringBody.endretAv()).isEqualTo(veilederId.getValue());
    }

    @Test
    void test_hent_huskelapp_for_veileder() throws Exception {
        Fnr fnr = Fnr.of("76543218457");
        EnhetId enhetId = EnhetId.of("1234");
        AktorId aktorId = AktorId.of("1223234234234");
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
        HuskelappOpprettRequest opprettRequest1 = new HuskelappOpprettRequest(fnr, LocalDate.now(), "Test", enhetId);
        HuskelappOpprettRequest opprettRequest2 = new HuskelappOpprettRequest(fnr, null, "Huskelapps", enhetId);
        testDataClient.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId);

        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId));
        when(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));

        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        );
        pdlIdentRepository.upsertIdenter(identer);

        String opprettetHuskelappId1Body = mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(opprettRequest1))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(201))
                .andReturn().getResponse().getContentAsString();
        HuskelappOpprettResponse huskelappId1Body = fromJson(opprettetHuskelappId1Body, HuskelappOpprettResponse.class);


        String opprettetHuskelappId2Body = mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(opprettRequest2))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(201))
                .andReturn().getResponse().getContentAsString();

        HuskelappOpprettResponse huskelappId2Body = fromJson(opprettetHuskelappId2Body, HuskelappOpprettResponse.class);

        HuskelappForVeilederRequest hentForVeilederRequest = new HuskelappForVeilederRequest(enhetId, veilederId);

        String result = mockMvc
                .perform(
                        post("/api/v1/hent-huskelapp-for-veileder")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(hentForVeilederRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();

        List<HuskelappResponse> hentetHuskelappEtterRedigeringBody = fromJson(result, new TypeReference<>() {
        });
        assertThat(hentetHuskelappEtterRedigeringBody.size()).isEqualTo(1);

        assertThat(hentetHuskelappEtterRedigeringBody.stream().anyMatch(var -> var.equals(new HuskelappResponse(
                huskelappId2Body.huskelappId(),
                opprettRequest2.brukerFnr(),
                opprettRequest2.enhetId(),
                opprettRequest2.frist(),
                opprettRequest2.kommentar(),
                LocalDate.now(),
                veilederId.getValue()
        )))).isTrue();
    }

    @Test
    void test_opprett_huskelapp_uten_kommentar_og_frist() throws Exception {
        Fnr fnr = Fnr.of("76543218457");
        EnhetId enhetId = EnhetId.of("1234");
        AktorId aktorId = AktorId.of("1223234234234");
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
        HuskelappOpprettRequest opprettRequest = new HuskelappOpprettRequest(fnr, null, null, enhetId);
        testDataClient.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId);

        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId));

        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        );
        pdlIdentRepository.upsertIdenter(identer);

        mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(opprettRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(400))
                .andReturn().getResponse().getContentAsString();

    }

    @Test
    void test_slett_huskelapp() throws Exception {
        Fnr fnr = Fnr.of("34567823456");
        EnhetId enhetId = EnhetId.of("1234");
        AktorId aktorId = AktorId.of("1223234234234");
        HuskelappOpprettRequest opprettRequest = new HuskelappOpprettRequest(fnr, LocalDate.now(), "Test", enhetId);

        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harTilgangTilPerson(any())).thenReturn(Decision.Permit.INSTANCE);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(veilarbPep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(AuthUtils.getInnloggetVeilederIdent()));
        when(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));

        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        );
        pdlIdentRepository.upsertIdenter(identer);

        String opprettetBody = mockMvc
                .perform(
                        post("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(opprettRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(201))
                .andReturn().getResponse().getContentAsString();
        HuskelappOpprettResponse opprettetResponse = fromJson(opprettetBody, HuskelappOpprettResponse.class);

        HuskelappSlettRequest slettRequest = new HuskelappSlettRequest(opprettetResponse.huskelappId());
        mockMvc
                .perform(
                        delete("/api/v1/huskelapp")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(slettRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(204));

        HuskelappForBrukerRequest hentForBrukerRequest = new HuskelappForBrukerRequest(fnr, enhetId);
        mockMvc
                .perform(
                        post("/api/v1/hent-huskelapp-for-bruker")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(hentForBrukerRequest))
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(200))
                .andExpect(content().string(""));
    }
}
