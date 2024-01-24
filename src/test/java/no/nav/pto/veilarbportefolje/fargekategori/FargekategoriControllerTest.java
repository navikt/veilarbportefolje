package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
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

import java.util.UUID;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FargekategoriController.class)
@Import(ApplicationConfigTest.class)
public class FargekategoriControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDataClient testDataClient;

    @MockBean
    private AuthService authService;

    private static final Fnr TESTBRUKER_FNR = Fnr.of("11111111111");
    private static final AktorId TESTBRUKER_AKTOR_ID = AktorId.of("99988877766655");
    private static final NavKontor TESTENHET = NavKontor.of("1234");
    private static final NavIdent TESTVEILEDER = NavIdent.of("Z999999");

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

        FargekategoriEntity fargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    TESTBRUKER_FNR.get());
        });

        assertThat(fargekategoriEntity).isNotNull();
        // id genereres så vi sjekker bare på tilstedeværelse
        assertThat(fargekategoriEntity.id()).isNotNull();
        assertThat(fargekategoriEntity.fnr()).isEqualTo(TESTBRUKER_FNR);
        assertThat(fargekategoriEntity.verdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_A);
        // sistEndret genereres så vi sjekker bare på tilstedeværelse
        assertThat(fargekategoriEntity.sistEndret()).isNotNull();
        assertThat(fargekategoriEntity.sistEndretAvVeilederIdent()).isEqualTo(TESTVEILEDER);
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

        testDataClient.lagreBrukerUnderOppfolging(TESTBRUKER_AKTOR_ID, TESTBRUKER_FNR, TESTENHET, VeilederId.of(TESTVEILEDER.get()));

        doNothing().when(authService).tilgangTilOppfolging();
        doNothing().when(authService).tilgangTilBruker(TESTBRUKER_FNR.get());
        doNothing().when(authService).tilgangTilEnhet(TESTENHET.getValue());
    }
}
