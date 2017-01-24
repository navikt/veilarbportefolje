package no.nav.fo.config;

import no.nav.fo.service.SolrService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@ComponentScan(basePackages = {"no.nav.fo.service"})
@PropertySource("classpath:veilarbportefolje.properties")
@Configuration
public class SolrConfig {

    @Bean
    public HttpSolrServer httpSolrServer() {
        return new HttpSolrServer(System.getProperty("veilarbportefolje.solr.masternode"));
    }

    @Bean
    public SolrService solrService() {
        return new SolrService();
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer properties = new PropertySourcesPlaceholderConfigurer();
        properties.setLocation(new ClassPathResource("veilarbportefolje.properties"));
        properties.setIgnoreResourceNotFound(false);
        return properties;
    }

    @Bean
    public Pingable solrServerPing() {
        HttpSolrServer server = new HttpSolrServer(System.getProperty("veilarbportefolje.solr.masternode"));
        return () -> {
            try {
                server.ping();
                return Pingable.Ping.lyktes("SolrServer");
            } catch (Exception e) {
                return Pingable.Ping.feilet("SolrServer", e);
            }
        };
    }
}
