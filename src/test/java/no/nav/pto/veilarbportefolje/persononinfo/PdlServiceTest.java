package no.nav.pto.veilarbportefolje.persononinfo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent;
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

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlServiceTest {
    private  PdlService pdlService;
    private PdlClient pdlClient;
    private JdbcTemplate db;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setup() {
        this.db =  SingletonPostgresContainer.init().createJdbcTemplate();
        String apiUrl = "http://localhost:" + wireMockRule.port();
        pdlClient = new PdlClientImpl(apiUrl, () -> "SYSTEM", () -> "SYSTEM");

        this.pdlService = new PdlService(new PdlRepository(db), pdlClient);
    }


    @Test
    public void lagreIdenterFraPdl() {
        AktorId aktorId = randomAktorId();
        givenThat(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(readFileAsJsonString("/identer_pdl.json", getClass())))
        );



        pdlService.lastInnIdenter(aktorId);
        List<PDLIdent> pdlIdenter = db.queryForList("select * from pdl_identer where bruker_nr = (select bruker_nr from pdl_identer where ident = ?)", aktorId.get())
                .stream()
                .map(this::mapTilident)
                .toList();
    }

    @SneakyThrows
    private PDLIdent mapTilident(Map<String, Object> rs) {
        return new PDLIdent()
                .setIdent((String) rs.get("ident"))
                .setHistorisk((Boolean) rs.get("historisk"))
                .setGruppe(PDLIdent.Gruppe.valueOf((String) rs.get("gruppe")));
    }
}
