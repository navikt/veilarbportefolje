package no.nav.fo.config.unleash;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class UnleashSpringConfig {

    public static final String UNLEASH_URL = "unleash.url";

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName("")
                .unleashApiUrl(getRequiredProperty(UNLEASH_URL))
                .build());
    }


}
