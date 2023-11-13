package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidsListeV2Controller;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteV2Request;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.Optional;

import static io.vavr.control.Validation.valid;
import static no.nav.common.json.JsonUtils.toJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArbeidsListeV2Controller.class)
@Import(ApplicationConfigTest.class)
public class ArbeidslisteV2ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArbeidslisteService arbeidslisteService;

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
    void test_at_vi_faar_serialisert_record_ArbeidslisteForBrukerRequest() throws Exception {
        Fnr fnr = Fnr.of("12345678910");
        AktorId aktorId = AktorId.of("1223234234234");
        Arbeidsliste arbeidsliste = new Arbeidsliste(
            VeilederId.of("Z12345"),
            ZonedDateTime.now(),
            "Tittel",
            "kommentar",
            ZonedDateTime.now(),
            true,
            true,
            Arbeidsliste.Kategori.LILLA,
            true,
            "1234"
            );



        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harTilgangTilPerson(any())).thenReturn(Decision.Permit.INSTANCE);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(veilarbPep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of("1234")));
        when(aktorClient.hentAktorId(any())).thenReturn(aktorId);
        when(arbeidslisteService.getArbeidsliste(aktorId)).thenReturn(Try.of(() -> arbeidsliste));


        mockMvc
                .perform(
                        post("/api/v2/hent-arbeidsliste")
                                .contentType(APPLICATION_JSON)
                                .content("{\"fnr\":\""+ fnr +"\"}")
                                .header("test_ident", "Z12345")
                                .header("test_ident_type", "INTERN")
                )
                .andExpect(status().is(200))
                .andExpect(content()
                .string(toJson(arbeidsliste)));
    }

    @Test
    void test_at_vi_faar_sendt_inn_Arbeidslister_uten_kommentar_og_tittel() throws Exception {
        Fnr fnr = Fnr.of("12345678910");
        VeilederId veilederId = VeilederId.of("Z12345");
        ArbeidslisteV2Request request = new ArbeidslisteV2Request(
                fnr, null, null, null, Arbeidsliste.Kategori.LILLA.name()
        );
        Arbeidsliste arbeidsliste = new Arbeidsliste(
                veilederId,
                ZonedDateTime.now(),
                null,
                null,
                ZonedDateTime.now(),
                true,
                true,
                Arbeidsliste.Kategori.LILLA,
                true,
                "1234"
        );

        ArbeidslisteDTO arbeidslisteDTO = new ArbeidslisteDTO(fnr);

        when(arbeidslisteService.createArbeidsliste(any())).thenReturn(Try.of(() -> arbeidslisteDTO));
        when(arbeidslisteService.erVeilederForBruker(fnr.get())).thenReturn(valid(Fnr.ofValidFnr(fnr.get())));
        when(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of("1234")));
        when(arbeidslisteService.getArbeidsliste(fnr)).thenReturn(Try.of(() -> arbeidsliste));

        mockMvc
                .perform(
                        post("/api/v2/arbeidsliste")
                                .contentType(APPLICATION_JSON)
                                .content(toJson(request))
                                .header("NAVident", "Z12345")
                                .header("test_ident_type", "INTERN")

                )
                .andExpect(status().is(200))
                .andExpect(content()
                        .string(toJson(arbeidsliste)));
    }
}
