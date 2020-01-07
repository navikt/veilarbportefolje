package no.nav.fo.veilarbportefolje.config;

import net.jodah.failsafe.RetryPolicy;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.fo.veilarbportefolje.krr.KrrDTO;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Optional;

import static java.time.Duration.ofSeconds;

@Configuration
public class ClientConfig {

    private static int retryDelayInSeconds = 10;
    private static int maxRetries = 3;

    public static int getRetryDelayInSeconds() {
        return retryDelayInSeconds;
    }

    public static void setRetryDelayInSeconds(int retryDelayInSeconds) {
        ClientConfig.retryDelayInSeconds = retryDelayInSeconds;
    }

    public static int getMaxRetries() {
        return maxRetries;
    }

    public static void setMaxRetries(int maxRetries) {
        ClientConfig.maxRetries = maxRetries;
    }

    @Bean
    public Client client() {
        Client client = RestUtils.createClient();
        client.register(new SystemUserOidcTokenProviderFilter());
        return client;
    }

    private static class SystemUserOidcTokenProviderFilter implements ClientRequestFilter {
        private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            clientRequestContext.getHeaders().putSingle("Authorization", "Bearer " + systemUserTokenProvider.getToken());
        }
    }

}
