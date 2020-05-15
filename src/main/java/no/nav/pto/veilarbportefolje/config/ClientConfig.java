package no.nav.pto.veilarbportefolje.config;

import no.nav.common.oidc.SystemUserTokenProvider;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class ClientConfig {

    @Bean
    public Client client() {
        Client client = RestUtils.createClient();
        client.register(new SystemUserOidcTokenProviderFilter());
        return client;
    }

    private static class SystemUserOidcTokenProviderFilter implements ClientRequestFilter {
        String discoveryUrl = getRequiredProperty("OPENAM_DISCOVERY_URL");
        String username = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
        String password = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);
        private SystemUserTokenProvider systemUserTokenProvider =
                new SystemUserTokenProvider(discoveryUrl, username, password);

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            clientRequestContext.getHeaders().putSingle("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserAccessToken());
        }
    }

}
