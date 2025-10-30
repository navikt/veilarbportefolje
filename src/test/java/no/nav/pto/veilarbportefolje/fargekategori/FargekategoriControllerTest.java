package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.client.AktorClient;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.util.TestDataClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.FARGEKATEGORI.*;
import static no.nav.pto.veilarbportefolje.fargekategori.FargekategoriControllerTestConfig.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FargekategoriController.class)
@Import({FargekategoriControllerTestConfig.class, ApplicationConfigTest.class})
public class FargekategoriControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDataClient testDataClient;

    @MockBean
    private AuthService authService;

    @MockBean
    private AktorClient aktorClient;

    @Test
    void henting_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        UUID uuid = UUID.randomUUID();
        Fnr fnr = TESTBRUKER_FNR;
        FargekategoriVerdi fargekategoriVerdi = FargekategoriVerdi.FARGEKATEGORI_D;
        LocalDate sistEndret = LocalDate.now();
        VeilederId sistEndretAv = AuthUtils.getInnloggetVeilederIdent();
        NavKontor navKontor = randomNavKontor();

        String request = """
                {
                  "fnr": "$fnr"
                }
                """.replace("$fnr", fnr.get());

        String fargekategoriSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(fargekategoriSql,
                uuid,
                fnr.get(),
                fargekategoriVerdi.name(),
                toTimestamp(sistEndret),
                sistEndretAv.getValue(),
                navKontor.getValue());

        String expected = """
                {
                    "id": "$uuid",
                    "fnr": "$fnr",
                    "fargekategoriVerdi": "$fargekategoriVerdi",
                    "sistEndret": "$sistEndret",
                    "endretAv": "$endretAvVeileder"
                }
                """.replace("$uuid", uuid.toString())
                .replace("$fnr", fnr.get())
                .replace("$fargekategoriVerdi", fargekategoriVerdi.name())
                .replace("$sistEndret", sistEndret.toString())
                .replace("$endretAvVeileder", sistEndretAv.getValue());

        mockMvc.perform(
                        post("/api/v1/hent-fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expected));
    }

    @Test
    void henting_av_fargekategori_skal_returnere_forventet_respons_når_bruker_ikke_har_fargekategori() throws Exception {
        Fnr fnr = TESTBRUKER_FNR;

        String request = """
                {
                  "fnr": "$fnr"
                }
                """.replace("$fnr", fnr.get());

        mockMvc.perform(
                        post("/api/v1/hent-fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().is(200))
                .andExpect(content().string(""));
    }

    @Test
    void batchoppretting_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        String opprettMangeRequest = """
                {
                  "fnr":["11111111111","22222222222","33333333333"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """;

        String expected = """
                {
                    "data": ["11111111111","22222222222","33333333333"],
                    "errors": [],
                    "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """;

        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expected));

        // TODO faktisk test forma på innhaldet
    }

    @Test
    void batchoppretting_av_fargekategori_skal_gi_riktig_tilstand_i_db() throws Exception {
        String fnr1 = TESTBRUKER_FNR.get();
        String fnr2 = TESTBRUKER2_FNR.get();
        String fnr3 = TESTBRUKER3_FNR.get();
        List<String> fnrliste = List.of(fnr1, fnr2, fnr3);

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(200));

        List<FargekategoriEntity> opprettedeFargekategorier = hentListeAvFargekategorier(fnrliste);

        assertThat(opprettedeFargekategorier.size()).isEqualTo(fnrliste.size());
        opprettedeFargekategorier.forEach(fargekategori ->
                assertThat(fargekategori.fargekategoriVerdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_B)
        );

        List<String> kategorifødselsnummer = opprettedeFargekategorier.stream().map(fargekategori -> fargekategori.fnr().get()).toList();
        fnrliste.forEach(fnr -> {
                    assertThat(kategorifødselsnummer.contains(fnr)).isTrue();
                }
        );
    }

    @Test
    void batchoppretting_av_fargekategori_skal_få_forventet_respons_når_alle_fnr_feiler_validering() throws Exception {
        String fnr1 = "1";
        String fnr2 = "dette er på ingen måte et fødselsnummer";
        String fnr3 = "dette er heller ikke et fødselsnummer";

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        String expected = """
                {
                    "data": [],
                    "errors": ["$fnr1", "$fnr2", "$fnr3"]
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);


        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(400))
                .andExpect(content().json(expected));
    }

    @Test
    void batchoppretting_av_fargekategori_skal_få_forventet_respons_når_noen_fnr_feiler_validering() throws Exception {
        String fnr1 = "1";
        String fnr2 = "dette er på ingen måte et fødselsnummer";
        String fnr3 = "11111111111";

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        String expected = """
                {
                    "data": ["$fnr3"],
                    "errors": ["$fnr1", "$fnr2"]
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);


        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expected));
    }

    @Test
    void batchoppretting_av_fargekategori_skal_få_forventet_respons_når_noen_fnr_feiler_validering_og_andre_feiler_autorisering() throws Exception {
        String fnr1 = "Dette er ikke et fødselsnummer";
        String fnr2 = "22222222222";
        String fnr3 = "44444444444";

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        String expected = """
                {
                    "data": ["$fnr2"],
                    "errors": ["$fnr1", "$fnr3"]
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);


        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expected));
    }

    @Test
    void batchoppretting_av_fargekategori_skal_få_forventet_respons_når_alle_fnr_feiler_validering_eller_autorisering() throws Exception {
        String fnr1 = "Dette er ikke et fødselsnummer";
        String fnr2 = "1";
        String fnr3 = "44444444444";

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        String expected = """
                {
                    "data": [],
                    "errors": ["$fnr1", "$fnr2", "$fnr3"]
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);


        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(403))
                .andExpect(content().json(expected));
    }


    @Test
    void batchoppretting_av_fargekategori_skal_få_forventet_respons_når_noen_fnr_feiler_autorisering() throws Exception {
        String fnr1 = TESTBRUKER_FNR.get();
        String fnr2 = TESTBRUKER2_FNR.get();
        String fnr3 = "1111";

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        String expected = """
                {
                    "data": ["$fnr1", "$fnr2"],
                    "errors": ["$fnr3"]
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);


        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(expected));
    }

    @Test
    void batchoppretting_av_fargekategori_skal_få_forventet_respons_når_alle_fnr_feiler_autorisering() throws Exception {
        String fnr1 = "44444444444";
        String fnr2 = "55555555555";
        String fnr3 = "66666666666";

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        String expected = """
                {
                    "data": [],
                    "errors": ["$fnr1", "$fnr2", "$fnr3"]
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3);

        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(403))
                .andExpect(content().json(expected));
    }

    @Test
    void batchoppdatering_av_fargekategori_skal_funke_når_en_bruker_allerede_har_fargekategori_og_andre_ikke() throws Exception {
        String fnr1 = TESTBRUKER_FNR.get();
        String fnr2 = TESTBRUKER2_FNR.get();
        String fnr3 = TESTBRUKER3_FNR.get();
        String TEST_ENHET = "1234";
        List<String> fnrliste = List.of(fnr1, fnr2, fnr3);

        FargekategoriVerdi fargekategoriVerdiFnr1Gammel = FargekategoriVerdi.FARGEKATEGORI_A;
        FargekategoriVerdi fargekategoriVerdiNy = FargekategoriVerdi.FARGEKATEGORI_B;

        String eksisterendeFargekategoriSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(eksisterendeFargekategoriSql,
                UUID.randomUUID(),
                fnr1,
                fargekategoriVerdiFnr1Gammel.name(),
                toTimestamp(LocalDate.now()),
                AuthUtils.getInnloggetVeilederIdent().getValue(),
                TEST_ENHET);

        String opprettMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"$fargekategori"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3)
                .replace("$fargekategori", fargekategoriVerdiNy.name());

        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettMangeRequest)
                )
                .andExpect(status().is(200));

        List<FargekategoriEntity> opprettedeFargekategorier = hentListeAvFargekategorier(fnrliste);

        assertThat(opprettedeFargekategorier.size()).isEqualTo(fnrliste.size());
        opprettedeFargekategorier.forEach(fargekategori ->
                assertThat(fargekategori.fargekategoriVerdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_B)
        );

        List<String> kategorifødselsnummer = opprettedeFargekategorier.stream().map(fargekategori -> fargekategori.fnr().get()).toList();
        fnrliste.forEach(fnr -> {
                    assertThat(kategorifødselsnummer.contains(fnr)).isTrue();
                }
        );
    }

    @Test
    void batchsletting_av_fargekategori_skal_funke() throws Exception {
        String fnr1 = TESTBRUKER_FNR.get();
        String fnr2 = TESTBRUKER2_FNR.get();
        String fnr3 = TESTBRUKER3_FNR.get();
        List<String> fnrliste = List.of(fnr1, fnr2, fnr3);

        FargekategoriVerdi fargekategoriVerdiFnr1Gammel = FargekategoriVerdi.FARGEKATEGORI_A;
        FargekategoriVerdi fargekategoriVerdiNy = FargekategoriVerdi.INGEN_KATEGORI;

        String eksisterendeFargekategoriSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(eksisterendeFargekategoriSql,
                UUID.randomUUID(),
                fnr1,
                fargekategoriVerdiFnr1Gammel.name(),
                toTimestamp(LocalDate.now()),
                AuthUtils.getInnloggetVeilederIdent().getValue());

        String deleteMangeRequest = """
                {
                  "fnr":["$fnr1","$fnr2","$fnr3"],
                  "fargekategoriVerdi":"$fargekategori"
                }
                """.replace("$fnr1", fnr1)
                .replace("$fnr2", fnr2)
                .replace("$fnr3", fnr3)
                .replace("$fargekategori", fargekategoriVerdiNy.name());

        mockMvc.perform(
                        put("/api/v1/fargekategorier")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(deleteMangeRequest)
                )
                .andExpect(status().is(200));

        List<FargekategoriEntity> fargekategorierIDatabase = hentListeAvFargekategorier(fnrliste);

        assertThat(fargekategorierIDatabase.size()).isEqualTo(0);
    }

    private List<FargekategoriEntity> hentListeAvFargekategorier(List<String> fnrliste) {
        return fnrliste.stream().map(fnr ->
                queryForObjectOrNull(() -> {
                    return jdbcTemplate.queryForObject(
                            "SELECT * FROM fargekategori WHERE fnr=?",
                            mapTilFargekategoriEntity(),
                            fnr);
                })
        ).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @NotNull
    private static RowMapper<FargekategoriEntity> mapTilFargekategoriEntity() {
        return (resultSet, rowNum) -> new FargekategoriEntity(
                UUID.fromString(resultSet.getString(ID)),
                Fnr.of(resultSet.getString(FNR)),
                FargekategoriVerdi.valueOf(resultSet.getString(VERDI)),
                toLocalDateOrNull(resultSet.getTimestamp(SIST_ENDRET)),
                NavIdent.of(resultSet.getString(SIST_ENDRET_AV_VEILEDERIDENT)),
                EnhetId.of(resultSet.getString(ENHET_ID))
        );
    }

    @BeforeEach
    void setup() {
        // Reset all data
        jdbcTemplate.update("TRUNCATE fargekategori");
        jdbcTemplate.update("TRUNCATE oppfolgingsbruker_arena_v2");

        VeilederId innloggetVeilederIdent = AuthUtils.getInnloggetVeilederIdent();

        testDataClient.lagreBrukerUnderOppfolging(TESTBRUKER_AKTOR_ID, TESTBRUKER_FNR, TESTENHET, innloggetVeilederIdent);
        testDataClient.lagreBrukerUnderOppfolging(TESTBRUKER2_AKTOR_ID, TESTBRUKER2_FNR, TESTENHET, innloggetVeilederIdent);
        testDataClient.lagreBrukerUnderOppfolging(TESTBRUKER3_AKTOR_ID, TESTBRUKER3_FNR, TESTENHET, innloggetVeilederIdent);

        when(aktorClient.hentAktorId(TESTBRUKER_FNR)).thenReturn(TESTBRUKER_AKTOR_ID);
        when(aktorClient.hentAktorId(TESTBRUKER2_FNR)).thenReturn(TESTBRUKER2_AKTOR_ID);
        when(aktorClient.hentAktorId(TESTBRUKER3_FNR)).thenReturn(TESTBRUKER3_AKTOR_ID);
        doNothing().when(authService).innloggetVeilederHarTilgangTilOppfolging();
        doNothing().when(authService).innloggetVeilederHarTilgangTilBruker(TESTBRUKER_FNR.get());
        doNothing().when(authService).innloggetVeilederHarTilgangTilEnhet(TESTENHET.getValue());
    }
}
