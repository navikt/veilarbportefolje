package no.nav.fo.veilarbportefolje.config;

import no.nav.fo.veilarbportefolje.service.VeilederService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

@Configuration
public class VeilederServiceConfig {
    @Bean
    public VeilederService veilederservice(Client restClient) {
        return new VeilederService(restClient);
    }
}
