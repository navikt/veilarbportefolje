package no.nav.fo.config;

import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import no.nav.fo.service.SolrServiceImpl;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
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
    public SolrService solrService(SolrClient solrClientMaster, SolrClient solrClientSlave, BrukerRepository brukerRepository, ArbeidslisteRepository arbeidslisteRepository, AktoerService aktoerService, AktivitetDAO aktivitetDAO) {
        return new SolrServiceImpl(solrClientMaster, solrClientSlave, brukerRepository, arbeidslisteRepository, aktoerService, aktivitetDAO);
    }

    @Bean
    public Pingable solrServerPing() {
        SolrClient solrClient = solrClientSlave();
        PingMetadata metadata = new PingMetadata("HTTP via " + System.getProperty("veilarbportefolje.solr.brukercore.url"),
                "Solr-indeks for portefolje",
                true
        );

        return () -> {
            try {
                solrClient.ping();
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
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
