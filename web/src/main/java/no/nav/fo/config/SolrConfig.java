package no.nav.fo.config;

import no.nav.fo.service.SolrService;
import no.nav.sbl.dialogarena.types.Pingable;
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

    @Bean
    public Pingable solrServerPing() {
        HttpSolrServer server = new HttpSolrServer(System.getProperty("no.nav.fo.brukercore.url"));
        return () -> {
            try {
                server.ping();
                return Pingable.Ping.lyktes("SolrServer");
            } catch (Exception e) {
                return Pingable.Ping.feilet("Feilet", e);
            }
        };
    }
}
