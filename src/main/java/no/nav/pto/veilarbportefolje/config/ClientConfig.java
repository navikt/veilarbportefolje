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
        OkHttpClient client = RestClient.baseClientBuilder()
        client.register(new SystemUserOidcTokenProviderFilter());
        return client;
    }
        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            clientRequestContext.getHeaders().putSingle("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserAccessToken());
        }
    }

}
