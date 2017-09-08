package no.nav.fo.config;

import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public BrukertilgangService sjekkBrukertilgang() {
        return new BrukertilgangService();
    }

    @Bean
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public ArbeidslisteService arbeidslisteService() {
        return new ArbeidslisteService();
    }

    @Bean
    public AktivitetService aktivitetService() {
        return new AktivitetService();
    }

    @Bean
    public TiltakService tiltakService() { return new TiltakService(); }

}
