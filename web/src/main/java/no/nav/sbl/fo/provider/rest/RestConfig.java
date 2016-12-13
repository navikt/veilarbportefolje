package no.nav.sbl.fo.provider.rest;

import no.nav.sbl.fo.provider.rest.logger.JSLoggerController;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestConfig extends ResourceConfig {

    public RestConfig() {
        super(
                DateTimeObjectMapperProvider.class,
                JSLoggerController.class
        );
    }
}
