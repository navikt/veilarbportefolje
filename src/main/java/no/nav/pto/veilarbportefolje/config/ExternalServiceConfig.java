package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.service.AktoerServiceImpl;
import no.nav.pto.veilarbportefolje.service.VirksomhetEnhetService;
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
