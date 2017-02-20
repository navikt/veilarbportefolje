package no.nav.fo.config;

import no.nav.fo.consumer.OppdaterBrukerdataListener;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        VirksomhetEnhetConfigTest.class,
        DatabaseConfigTest.class,
})
public class ApplicationConfigTest {

    @Bean
    public AktoerV2 aktoerV2() { return mock(AktoerV2.class); }

    @Bean
    public BrukertilgangService brukertilgangService() { return new BrukertilgangService(); }

    @Bean
    public BrukerRepository brukerRepository() {
        return new BrukerRepository();
    }

    @Bean
    public OppdaterBrukerdataFletter oppdaterBrukerdataFletter() { return new OppdaterBrukerdataFletter(); }

    @Bean
    public OppdaterBrukerdataListener oppdaterBrukerdataListener() { return new OppdaterBrukerdataListener(); }
}
