package no.nav.fo.veilarbportefolje.krr;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.common.auth.TestSubjectUtils;
import no.nav.fo.veilarbportefolje.FailSafeConfig;
import no.nav.fo.veilarbportefolje.config.ClientConfig;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import no.nav.sbl.sql.SqlUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.auth.SsoToken.Type.OIDC;
import static no.nav.fo.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbportefolje.database.KrrTabell.KRR;
import static no.nav.fo.veilarbportefolje.database.KrrTabell.Kolonne.FODSELSNR;
import static no.nav.fo.veilarbportefolje.krr.KrrService.DKIF_URL_PATH;
import static no.nav.fo.veilarbportefolje.krr.KrrService.DKIF_URL_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class KrrServiceTest {

    private KrrService krrService;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    private JdbcTemplate db;

    private static final String jsonBody = "{\n" +
            "  \"kontaktinfo\": {\n" +
            "    \"{{fnr}}\": {\n" +
            "      \"personident\": \"10101010101\",\n" +
            "      \"reservert\": true\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    private static final String FNR = "10101010101";

    private String DKIF_URL = DKIF_URL_PATH + "?inkluderSikkerDigitalPost=false";

    @Before
    public void setUp() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);

        KrrRepository krrRepository = new KrrRepository(db);
        krrRepository.slettKrrInformasjon();

        krrService = new KrrService(krrRepository);

        systemPropertiesRule.setProperty(DKIF_URL_PROPERTY_NAME, "http://localhost:" + wireMockRule.port());
        subjectRule.setSubject(TestSubjectUtils.builder().tokenType(OIDC).token("test").build());

    }

    @Test
    public void skal_hente_krr_kontaktinformasjon_og_oppdatere_database() {

        stubFor(
                get(urlEqualTo(DKIF_URL))
                        .willReturn(ok().withHeader("Content-Type", "application/json").withBody(jsonBody))
        );

        krrService.oppdaterKrrInfo(singletonList(FNR));

        String result = SqlUtils.select(db, KRR, rs -> rs.getString(FODSELSNR))
                .column(FODSELSNR)
                .execute();

        assertThat(result).isEqualTo(FNR);
    }

    @Test
    public void skal_proeve_igjen_ved_feil_mot_dkif() {
        String retryScenario = "retry_scenario";

        stubFor(
                get(urlEqualTo(DKIF_URL)).inScenario(retryScenario)
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                        .willSetStateTo("RETRY")
        );

        stubFor(
                get(urlEqualTo(DKIF_URL)).inScenario(retryScenario)
                        .whenScenarioStateIs("RETRY")
                        .willReturn(ok().withHeader("Content-Type", APPLICATION_JSON).withBody(jsonBody))
        );

        ClientConfig.setDefaultFailsafeConfig(
                FailSafeConfig.builder()
                        .maxRetries(3)
                        .retryDelay(Duration.ofMillis(1))
                        .timeout(ofSeconds(100))
                        .build()
        );

        krrService.oppdaterKrrInfo(singletonList(FNR));
    }
}
