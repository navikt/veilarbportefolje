package no.nav.fo.config;

import no.nav.fo.service.SolrService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
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
    public SolrClient solrClientSlave() {
        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(System.getProperty("veilarbportefolje.solr.brukercore.url"))
                .withHttpClient(createHttpClientForSolr())
                .build();
    }

    @Bean
    public SolrClient solrClientMaster() {
        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(System.getProperty("veilarbportefolje.solr.masternode"))
                .withHttpClient(createHttpClientForSolr())
                .build();
    }

    @Bean
    public SolrService solrService() {
        return new SolrService();
    }

    @Bean
    public Pingable solrServerPing() {
        SolrClient solrClient = new HttpSolrClient.Builder()
                .withBaseSolrUrl(System.getProperty("veilarbportefolje.solr.masternode"))
                .withHttpClient(createHttpClientForSolr())
                .build();

        return () -> {
            try {
                solrClient.ping();
                return Pingable.Ping.lyktes("SolrServer");
            } catch (Exception e) {
                return Pingable.Ping.feilet("SolrServer", e);
            }
        };
    }

    private HttpClient createHttpClientForSolr() {
        String username = System.getProperty("no.nav.modig.security.systemuser.username");
        String password = System.getProperty("no.nav.modig.security.systemuser.password");

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .addInterceptorFirst(new PreemptiveAuthInterceptor())
                .build();
    }

    private class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() == null) {
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                Credentials credentials = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                if (credentials == null) {
                    throw new HttpException("No credentials provided for preemptive authentication.");
                }
                authState.update(new BasicScheme(), credentials);
            }

        }
    }
}
