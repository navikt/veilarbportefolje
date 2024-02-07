package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.TestDataClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.FARGEKATEGORI.*;
import static no.nav.pto.veilarbportefolje.fargekategori.FargekategoriControllerTestConfig.TESTBRUKER_AKTOR_ID;
import static no.nav.pto.veilarbportefolje.fargekategori.FargekategoriControllerTestConfig.TESTBRUKER_FNR;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FargekategoriController.class)
@Import({ApplicationConfigTest.class, FargekategoriControllerTestConfig.class})
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

    @Qualifier("fargekategoriControllerTestAuthService")
    private AuthContextHolder authContextHolder;

    @Test
    void henting_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        UUID uuid = UUID.randomUUID();
        Fnr fnr = TESTBRUKER_FNR;
        FargekategoriVerdi fargekategoriVerdi = FargekategoriVerdi.FARGEKATEGORI_D;
        LocalDate sistEndret = LocalDate.now();
        VeilederId sistEndretAv = AuthUtils.getInnloggetVeilederIdent();

        String request = """
                {
                  "fnr": "$fnr"
                }
                """.replace("$fnr", fnr.get());

        String fargekategoriSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(fargekategoriSql,
                uuid,
                fnr.get(),
                fargekategoriVerdi.name(),
                toTimestamp(sistEndret),
                sistEndretAv.getValue());

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
    void opprettelse_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        String request = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"FARGEKATEGORI_A"
                }
                """;

        String responseJson = mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();

        assertThat(responseJson.contains("\"id\":")).isTrue();
    }

    @Test
    void opprettelse_av_fargekategori_skal_gi_riktig_tilstand_i_db() throws Exception {
        String request = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"FARGEKATEGORI_A"
                }
                """;

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().is(200));

        FargekategoriEntity opprettetFargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    TESTBRUKER_FNR.get());
        });

        assertThat(opprettetFargekategoriEntity).isNotNull();
        // id genereres så vi sjekker bare på tilstedeværelse
        assertThat(opprettetFargekategoriEntity.id()).isNotNull();
        assertThat(opprettetFargekategoriEntity.fnr()).isEqualTo(TESTBRUKER_FNR);
        assertThat(opprettetFargekategoriEntity.fargekategoriVerdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_A);
        // sistEndret genereres så vi sjekker bare på tilstedeværelse
        assertThat(opprettetFargekategoriEntity.sistEndret()).isNotNull();
        assertThat(opprettetFargekategoriEntity.endretAv()).isEqualTo(FargekategoriControllerTestConfig.TESTVEILEDER);
    }

    @Test
    void oppdatering_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        String opprettRequest = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"FARGEKATEGORI_A"
                }
                """;
        String oppdaterRequest = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """;

        String opprettJson = mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettRequest)
                )
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(oppdaterRequest)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(opprettJson));
    }

    @Test
    void oppdatering_av_fargekategori_skal_gi_riktig_tilstand_i_db() throws Exception {
        UUID uuid = UUID.randomUUID();
        Fnr fnr = TESTBRUKER_FNR;
        FargekategoriVerdi fargekategoriVerdi = FargekategoriVerdi.FARGEKATEGORI_D;
        LocalDate sistEndret = LocalDate.now().minusDays(1);
        VeilederId sistEndretAv = AuthUtils.getInnloggetVeilederIdent();

        String fargekategoriSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(fargekategoriSql,
                uuid,
                fnr.get(),
                fargekategoriVerdi.name(),
                toTimestamp(sistEndret),
                sistEndretAv.getValue());

        String oppdaterRequest = """
                {
                  "fnr":"$fnr",
                  "fargekategoriVerdi":"FARGEKATEGORI_B"
                }
                """
                .replace("$fnr", fnr.get());

        FargekategoriEntity opprettetFargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    TESTBRUKER_FNR.get());
        });

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(oppdaterRequest)
                )
                .andExpect(status().is(200));

        FargekategoriEntity oppdatertFargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    TESTBRUKER_FNR.get());
        });

        assertThat(oppdatertFargekategoriEntity).isNotNull();
        assertThat(opprettetFargekategoriEntity).isNotNull();
        assertThat(oppdatertFargekategoriEntity.id()).isEqualTo(opprettetFargekategoriEntity.id());
        assertThat(oppdatertFargekategoriEntity.fnr()).isEqualTo(opprettetFargekategoriEntity.fnr());
        assertThat(oppdatertFargekategoriEntity.endretAv()).isEqualTo(opprettetFargekategoriEntity.endretAv());
        assertThat(oppdatertFargekategoriEntity.sistEndret()).isNotEqualTo(opprettetFargekategoriEntity.sistEndret());
        assertThat(oppdatertFargekategoriEntity.fargekategoriVerdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_B);
    }

    @Test
    void sletting_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        String opprettRequest = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"FARGEKATEGORI_A"
                }
                """;
        String slettRequest = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"INGEN_KATEGORI"
                }
                """;

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettRequest)
                )
                .andExpect(status().is(200));

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(slettRequest)
                )
                .andExpect(status().is(204));
    }

    @Test
    void sletting_av_fargekategori_skal_gi_riktig_tilstand_i_db() throws Exception {
        String opprettRequest = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"FARGEKATEGORI_A"
                }
                """;
        String slettRequest = """
                {
                  "fnr":"11111111111",
                  "fargekategoriVerdi":"INGEN_KATEGORI"
                }
                """;

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettRequest)
                )
                .andExpect(status().is(200));

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(slettRequest)
                )
                .andExpect(status().is(204));

        FargekategoriEntity oppdatertFargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    TESTBRUKER_FNR.get());
        });

        assertThat(oppdatertFargekategoriEntity).isNull();
    }

    @NotNull
    private static RowMapper<FargekategoriEntity> mapTilFargekategoriEntity() {
        return (resultSet, rowNum) -> new FargekategoriEntity(
                UUID.fromString(resultSet.getString(ID)),
                Fnr.of(resultSet.getString(FNR)),
                FargekategoriVerdi.valueOf(resultSet.getString(VERDI)),
                toLocalDate(resultSet.getTimestamp(SIST_ENDRET)),
                NavIdent.of(resultSet.getString(SIST_ENDRET_AV_VEILEDERIDENT))
        );
    }


    @BeforeEach
    void setup() {
        // Reset all data
        jdbcTemplate.update("TRUNCATE fargekategori");
        jdbcTemplate.update("TRUNCATE oppfolgingsbruker_arena_v2");

        testDataClient.lagreBrukerUnderOppfolging(TESTBRUKER_AKTOR_ID, TESTBRUKER_FNR, FargekategoriControllerTestConfig.TESTENHET, VeilederId.of(FargekategoriControllerTestConfig.TESTVEILEDER.get()));

        when(aktorClient.hentAktorId(TESTBRUKER_FNR)).thenReturn(TESTBRUKER_AKTOR_ID);
        doNothing().when(authService).tilgangTilOppfolging();
        doNothing().when(authService).tilgangTilBruker(TESTBRUKER_FNR.get());
        doNothing().when(authService).tilgangTilEnhet(FargekategoriControllerTestConfig.TESTENHET.getValue());
    }
}
