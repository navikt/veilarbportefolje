package no.nav.fo.smoketest;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.TokenUtils;
import no.nav.brukerdialog.security.oidc.UserTokenProvider;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.sbl.util.PropertyUtils;
import org.junit.jupiter.api.BeforeAll;

import javax.ws.rs.core.Cookie;
import java.util.Objects;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Smoketest {
    public static String INNLOGGET_VEILEDER;
    public static Cookie tokenCookie;
    public static String MILJO;
    public static String HOSTNAME;

    private static final String ID_TOKEN = "ID_token";

    @BeforeAll
    public static void setupSecirity() {
        MILJO = getRequiredProperty("miljo");
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig("veilarbportefolje"));

        UserTokenProvider userTokenProvider = new UserTokenProvider();
        OidcCredential token = userTokenProvider.getIdToken();
        InternbrukerSubjectHandler.setOidcCredential(token);
        InternbrukerSubjectHandler.setVeilederIdent(TokenUtils.getTokenSub(token.getToken()));
        tokenCookie = new Cookie(ID_TOKEN, token.getToken());

        INNLOGGET_VEILEDER = TokenUtils.getTokenSub(token.getToken());
        HOSTNAME = getHostname();
    }

    private static String getHostname() {
        return Objects.nonNull(MILJO) ? String.format("https://app-%s.adeo.no/", MILJO) : "http://localhost:8080/";

    }
}
