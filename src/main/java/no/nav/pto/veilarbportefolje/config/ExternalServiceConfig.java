package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.service.AktoerServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalServiceConfig {

    @Bean
    public AktoerService aktoerService() { return new AktoerServiceImpl(); }


}
