package no.nav.pto.veilarbportefolje.arbeidsliste;

import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidsListeV2Controller;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteForBrukerRequest;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteV2Request;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.generateJWT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = ArbeidsListeV2Controller.class)
@Import(ApplicationConfigTest.class)
public class ArbeidslisteV2ControllerIntegrationTest {

    private static final String TEST_FNR = "01234567890";
    private static final String TEST_ENHETSID = "1234";
    private static final String TEST_VEILEDERIDENT = "Z123456";
    private static final String TEST_AKTORID = "123456789";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthContextHolder authContextHolder;
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
    public void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet() throws Exception {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    null,
                                    null,
                                    null,
                                    Arbeidsliste.Kategori.LILLA.name()))))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"123456789\"}"));
        });
    }

    @Test
    public void slett_arbeidsliste_skal_fjerne_arbeidsliste_som_forventet() throws Exception {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    null,
                                    null,
                                    null,
                                    Arbeidsliste.Kategori.LILLA.name()))))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR))))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    public void oppdater_arbeidsliste_skal_oppdatere_arbeidsliste_som_forventet() throws Exception {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    null,
                                    null,
                                    null,
                                    Arbeidsliste.Kategori.LILLA.name()))))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.put("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    "En flott tittel",
                                    "Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.",
                                    null,
                                    Arbeidsliste.Kategori.GRONN.name())))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"123456789\"}"));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"123456789\"}"));
        });
    }

    @BeforeEach
    public void setupMocks() {
        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harTilgangTilPerson(any())).thenReturn(Decision.Permit.INSTANCE);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(veilarbPep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        Mockito.doNothing().when(authService).tilgangTilOppfolging();
        when(brukerService.hentVeilederForBruker(AktorId.of(TEST_AKTORID))).thenReturn(Optional.of(VeilederId.of(
                TEST_VEILEDERIDENT)));
        when(brukerService.hentNavKontor(Fnr.of(TEST_FNR))).thenReturn(Optional.of(NavKontor.of(TEST_ENHETSID)));
        when(aktorClient.hentAktorId(Fnr.of(TEST_FNR))).thenReturn(AktorId.of(TEST_AKTORID));
        when(brukerService.hentAktorId(Fnr.of(TEST_FNR))).thenReturn(Optional.of(AktorId.of(TEST_AKTORID)));
    }
}
