package no.nav.fo.config;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.service.BrukertilgangService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VirksomhetEnhetConfigTest.class,
        DatabaseConfigTest.class
})
public class ApplicationConfigTest {

    @Bean
    public BrukertilgangService brukertilgangService() { return new BrukertilgangService(); }

    @Bean
    public BrukerRepository brukerRepository() {
        return new BrukerRepository();
    }
}
