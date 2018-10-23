package no.nav.fo.veilarbportefolje.config;

import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.fo.veilarbportefolje.service.AktoerServiceImpl;
import no.nav.fo.veilarbportefolje.service.VirksomhetEnhetService;
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
