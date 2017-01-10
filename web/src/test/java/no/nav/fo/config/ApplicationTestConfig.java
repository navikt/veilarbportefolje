package no.nav.fo.config;

import no.nav.fo.config.endpoints.VirksomhetEnhetTestConfig;
import no.nav.fo.service.SjekkBrukertilgang;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VirksomhetEnhetTestConfig.class
})
public class ApplicationTestConfig {

    @Bean
    public SjekkBrukertilgang sjekkBrukertilgang() { return new SjekkBrukertilgang(); }
}
