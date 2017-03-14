package no.nav.fo.config;


import no.nav.fo.routes.KopierGR199FraArenaRoute;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

    @Bean
    public KopierGR199FraArenaRoute kopierGR199FraArenaRoute() {
        return new KopierGR199FraArenaRoute();
    }

    @Bean
    public DefaultCamelContext camelContext(KopierGR199FraArenaRoute kopierRute) throws Exception {
        DefaultCamelContext defaultCamelContext = new DefaultCamelContext();
        defaultCamelContext.setTracing(true);
        defaultCamelContext.getProperties().put(Exchange.LOG_DEBUG_BODY_STREAMS, "true");

        defaultCamelContext.addRoutes(kopierRute);
        defaultCamelContext.start();

        return defaultCamelContext;
    }

}
