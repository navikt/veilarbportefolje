package no.nav.fo.config;

import no.nav.fo.service.AktoerService;
import no.nav.fo.service.AktoerServiceImpl;
import no.nav.fo.service.VirksomhetEnhetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalServiceConfig {

    @Bean
    public VirksomhetEnhetService virksomhetEnhetServiceImpl() {
        return new VirksomhetEnhetService();
    }

    @Bean
    public AktoerService aktoerService() { return new AktoerServiceImpl(); }


}
