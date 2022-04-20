package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentRespons;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.junit.Assert.assertTrue;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private PdlService pdlService;
    private JdbcTemplate db;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setup() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
        db.update("truncate pdl_identer");

        String apiUrl = "http://localhost:" + wireMockRule.port();
        this.pdlService = new PdlService(
                new PdlRepository(db),
                new PdlClientImpl(apiUrl, () -> "SYSTEM", () -> "SYSTEM")
        );
    }

    @Test
    @SneakyThrows
    public void lagreIdenterFraPdl() {
        String pdlResponsJson = readFileAsJsonString("/identer_pdl.json", getClass());
        givenThat(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlResponsJson))
        );

        var identerFrafil = mapper.readValue(pdlResponsJson, PdlIdentRespons.class)
                .getData()
                .getHentIdenter()
                .getIdenter();

        pdlService.lastInnIdenter(randomAktorId());
        List<PDLIdent> identerFraPostgres = db.queryForList("select * from pdl_identer")
                .stream()
                .map(PdlServiceTest::mapTilident)
                .toList();

        identerFrafil.forEach(filIdent ->
                assertTrue(identerFraPostgres.contains(filIdent))
        );
    }

    @SneakyThrows
    private static PDLIdent mapTilident(Map<String, Object> rs) {
        return new PDLIdent()
                .setIdent((String) rs.get("ident"))
                .setHistorisk((Boolean) rs.get("historisk"))
                .setGruppe(PDLIdent.Gruppe.valueOf((String) rs.get("gruppe")));
    }
}
