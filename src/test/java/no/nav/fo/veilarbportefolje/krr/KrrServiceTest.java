package no.nav.fo.veilarbportefolje.krr;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.common.auth.TestSubjectUtils;
import no.nav.fo.veilarbportefolje.database.KrrTabell;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import no.nav.sbl.sql.SqlUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.singletonList;
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

    @Before
    public void setUp() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        KrrRepository krrRepository = new KrrRepository(db);
        krrService = new KrrService(krrRepository);

        systemPropertiesRule.setProperty(DKIF_URL_PROPERTY_NAME, "http://localhost:" + wireMockRule.port());
        subjectRule.setSubject(TestSubjectUtils.builder().tokenType(OIDC).token("test").build());
    }

    @Test
    public void skal_hente_krr_kontaktinformasjon_og_oppdatere_database() {

        String jsonBody = "{\n" +
                "  \"kontaktinfo\": {\n" +
                "    \"{{fnr}}\": {\n" +
                "      \"personident\": \"10101010101\",\n" +
                "      \"reservert\": true\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        stubFor(
                get(urlEqualTo(DKIF_URL_PATH + "?inkluderSikkerDigitalPost=false"))
                        .willReturn(ok().withHeader("Content-Type", "application/json").withBody(jsonBody))
        );

        String fnr = "10101010101";

        krrService.oppdaterKrrInfo(singletonList(fnr));

        String result = SqlUtils.select(db, KRR, rs -> rs.getString(FODSELSNR))
                .column(FODSELSNR)
                .execute();

        assertThat(result).isEqualTo(fnr);
    }
}
