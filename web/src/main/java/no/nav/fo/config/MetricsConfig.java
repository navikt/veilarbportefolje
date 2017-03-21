package no.nav.fo.config;


import no.nav.metrics.aspects.CountAspect;
import no.nav.metrics.aspects.TimerAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public TimerAspect timerAspect() {
        return new TimerAspect();
    }

    @Bean
    public CountAspect countAspect() {
        return new CountAspect();
    }
}
