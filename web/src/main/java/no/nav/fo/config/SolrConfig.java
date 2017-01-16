package no.nav.fo.config;

import no.nav.fo.service.SolrService;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfig {

    @Bean
    public HttpSolrServer httpSolrServer() {
        return new HttpSolrServer(System.getProperty("no.nav.fo.brukercore.url"));
    }

    @Bean
    public SolrService solrService() {
        return new SolrService();
    }
}
