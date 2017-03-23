package no.nav.fo.config;

import no.nav.fo.service.SolrService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
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
    public SolrClient solrClient() {
        String username = System.getProperty("no.nav.modig.security.systemuser.username");
        String password = System.getProperty("no.nav.modig.security.systemuser.password");

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .addInterceptorFirst(new PreemptiveAuthInterceptor())
                .build();

        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(System.getProperty("veilarbportefolje.solr.masternode"))
                .withHttpClient(httpClient)
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
