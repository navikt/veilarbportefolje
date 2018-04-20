package no.nav.fo.config;

import no.nav.fo.service.VeilederService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VeilederServiceConfig {
    @Bean
    public VeilederService veilederservice() {
        return new VeilederService();
    }
}
