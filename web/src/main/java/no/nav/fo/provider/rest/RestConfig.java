package no.nav.fo.provider.rest;

import no.nav.fo.provider.rest.logger.JSLoggerController;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestConfig extends ResourceConfig {

    public RestConfig() {
        super(
                DateTimeObjectMapperProvider.class,
                JSLoggerController.class,
                HentPortefoljeForEnhetController.class,
                SolrController.class
        );
    }
}
