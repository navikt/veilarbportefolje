package no.nav.fo.veilarbportefolje.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

@Slf4j
@Configuration
public class HttpConfig {

    @Bean
    public Client client() {
        Client client = RestUtils.createClient();
        client.register(new SystemUserOidcTokenProviderFilter());
        return client;
    }

    private static class SystemUserOidcTokenProviderFilter implements ClientRequestFilter {
        private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

        @Override
        public void filter(ClientRequestContext clientRequestContext) {
            clientRequestContext.getHeaders().putSingle("Authorization", "Bearer " + systemUserTokenProvider.getToken());
        }
    }

}
