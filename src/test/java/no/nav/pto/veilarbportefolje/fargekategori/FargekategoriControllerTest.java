package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FargekategoriController.class)
@Import(ApplicationConfigTest.class)
public class FargekategoriControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void test_opprettelse_av_fargekategori() throws Exception {
        // TODO: Sett opp database med oppfølgingsbruker, mm.

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

        // TODO: Sjekke at resultatet i DB er som forventet
        FargekategoriEntity fargekategoriEntity = queryForObjectOrNull(() -> {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM fargekategori WHERE fnr=?",
                    mapTilFargekategoriEntity(),
                    "11111111111");
        });
        assertThat(fargekategoriEntity).isNotNull();
        assertThat(fargekategoriEntity.id()).isNotNull();
        assertThat(fargekategoriEntity.fnr()).isEqualTo(Fnr.of("11111111111"));
        assertThat(fargekategoriEntity.verdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_A);
        assertThat(fargekategoriEntity.sistEndret()).isNotNull();
        assertThat(fargekategoriEntity.sistEndretAvVeilederIdent()).isEqualTo(NavIdent.of("Z999999"));
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
}
