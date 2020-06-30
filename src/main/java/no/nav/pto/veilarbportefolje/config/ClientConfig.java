package no.nav.pto.veilarbportefolje.config;

import no.nav.common.rest.client.RestClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;


@Configuration
public class ClientConfig {

    @Bean
    public Client client() {
        OkHttpClient client = RestClient.baseClient();
        client.register(new SystemUserOidcTokenProviderFilter());
        return client;
    }

    private static class SystemUserOidcTokenProviderFilter implements ClientRequestFilter {
        String discoveryUrl = getRequiredProperty("SECURITY_TOKEN_SERVICE_OPENID_CONFIGURATION_URL");
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
