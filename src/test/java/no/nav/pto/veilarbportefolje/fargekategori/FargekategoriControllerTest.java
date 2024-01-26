package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
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

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
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

    @Qualifier("fargekategoriControllerTestAuthService")
    private AuthContextHolder authContextHolder;

    @Test
    void henting_av_fargekategori_skal_returnere_forventet_respons() throws Exception {
        UUID uuid = UUID.randomUUID();
        Fnr fnr = Fnr.of("11223312345");
        FargekategoriVerdi fargekategoriVerdi = FargekategoriVerdi.FARGEKATEGORI_D;
        ZonedDateTime sistEndret = ZonedDateTime.now();
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
                            Timestamp.from(sistEndret.toInstant()),
                            sistEndretAv.getValue());

        String expected = """
                {
                    "id": "$uuid",
                    "fnr": "$fnr",
                    "verdi": "$fargekategoriVerdi",
                    "sistEndret": "$sistEndret",
                    "sistEndretAvVeilederIdent": "$endretAvVeileder"
                }
                """.replace("$uuid", uuid.toString())
                .replace("$fnr", fnr.get())
                .replace("$fargekategoriVerdi", fargekategoriVerdi.name())
                .replace("$sistEndret", sistEndret.toString().substring(0, 32)) // substring gjer at vi unngår å få med [Paris/Oslo] i strengen
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
    void skal_ikke_kunne_hente_fargekategori_uten_riktig_autentisering() throws Exception {
        // TODO skriv testar
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
                    FargekategoriControllerTestConfig.TESTBRUKER_FNR.get());
        });

        assertThat(opprettetFargekategoriEntity).isNotNull();
        // id genereres så vi sjekker bare på tilstedeværelse
        assertThat(opprettetFargekategoriEntity.id()).isNotNull();
        assertThat(opprettetFargekategoriEntity.fnr()).isEqualTo(FargekategoriControllerTestConfig.TESTBRUKER_FNR);
        assertThat(opprettetFargekategoriEntity.verdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_A);
        // sistEndret genereres så vi sjekker bare på tilstedeværelse
        assertThat(opprettetFargekategoriEntity.sistEndret()).isNotNull();
        assertThat(opprettetFargekategoriEntity.sistEndretAvVeilederIdent()).isEqualTo(FargekategoriControllerTestConfig.TESTVEILEDER);
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

        mockMvc.perform(
                        put("/api/v1/fargekategori")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(opprettRequest)
                )
                .andExpect(status().is(200));

        FargekategoriEntity opprettetFargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    FargekategoriControllerTestConfig.TESTBRUKER_FNR.get());
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
                    FargekategoriControllerTestConfig.TESTBRUKER_FNR.get());
        });

        assertThat(oppdatertFargekategoriEntity).isNotNull();
        assertThat(opprettetFargekategoriEntity).isNotNull();
        assertThat(oppdatertFargekategoriEntity.id()).isEqualTo(opprettetFargekategoriEntity.id());
        assertThat(oppdatertFargekategoriEntity.fnr()).isEqualTo(opprettetFargekategoriEntity.fnr());
        assertThat(oppdatertFargekategoriEntity.sistEndretAvVeilederIdent()).isEqualTo(opprettetFargekategoriEntity.sistEndretAvVeilederIdent());
        assertThat(oppdatertFargekategoriEntity.sistEndret()).isNotEqualTo(opprettetFargekategoriEntity.sistEndret());
        assertThat(oppdatertFargekategoriEntity.verdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_B);
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
                    FargekategoriControllerTestConfig.TESTBRUKER_FNR.get());
        });

        assertThat(oppdatertFargekategoriEntity).isNull();
    }

    @NotNull
    private static RowMapper<FargekategoriEntity> mapTilFargekategoriEntity() {
        return (resultSet, rowNum) -> new FargekategoriEntity(
                UUID.fromString(resultSet.getString("id")),
                Fnr.of(resultSet.getString("fnr")),
                FargekategoriVerdi.valueOf(resultSet.getString("verdi")),
                DateUtils.toZonedDateTime(resultSet.getTimestamp("sist_endret")),
                NavIdent.of(resultSet.getString("sist_endret_av_veilederident"))
        );
    }


    @BeforeEach
    void setup() {
        // Reset all data
        jdbcTemplate.update("TRUNCATE fargekategori");
        jdbcTemplate.update("TRUNCATE oppfolgingsbruker_arena_v2");

        testDataClient.lagreBrukerUnderOppfolging(FargekategoriControllerTestConfig.TESTBRUKER_AKTOR_ID, FargekategoriControllerTestConfig.TESTBRUKER_FNR, FargekategoriControllerTestConfig.TESTENHET, VeilederId.of(FargekategoriControllerTestConfig.TESTVEILEDER.get()));

        doNothing().when(authService).tilgangTilOppfolging();
        doNothing().when(authService).tilgangTilBruker(FargekategoriControllerTestConfig.TESTBRUKER_FNR.get());
        doNothing().when(authService).tilgangTilEnhet(FargekategoriControllerTestConfig.TESTENHET.getValue());
    }
}
