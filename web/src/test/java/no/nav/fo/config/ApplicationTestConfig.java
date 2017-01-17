package no.nav.fo.config;

import no.nav.fo.service.BrukertilgangService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VirksomhetEnhetConfigTest.class
})
public class ApplicationTestConfig {

    @Bean
    public BrukertilgangService brukertilgangService() { return new BrukertilgangService(); }
}
