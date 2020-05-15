package no.nav.pto.veilarbportefolje.feed;

import no.nav.common.oidc.SystemUserTokenProvider;
import no.nav.fo.feed.common.OutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import javax.ws.rs.client.Invocation;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class OidcFeedOutInterceptor implements OutInterceptor {

    String discoveryUrl = getRequiredProperty("SECURITY_TOKEN_SERVICE_OPENID_CONFIGURATION_URL");
    String username = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    String password = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);
    private SystemUserTokenProvider systemUserTokenProvider =
            new SystemUserTokenProvider(discoveryUrl, username, password);

    @Override
    public void apply(Invocation.Builder builder) {
        builder.header("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserAccessToken());
    }
}
