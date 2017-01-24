package no.nav.fo.config;

import no.nav.fo.service.SolrService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
public class SolrConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertiesConfig = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();
        properties.put("veilarbportefolje.cron.hovedindeksering", System.getProperty("veilarbportefolje.cron.hovedindeksering"));
        properties.put("veilarbportefolje.cron.deltaindeksering", System.getProperty("veilarbportefolje.cron.deltaindeksering"));
        propertiesConfig.setProperties(properties);
        return propertiesConfig;
    }

    @Bean
    public HttpSolrServer httpSolrServer() {
        return new HttpSolrServer(System.getProperty("veilarbportefolje.solr.masternode"));
    }

    @Bean
    public SolrService solrService() {
        return new SolrService();
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
