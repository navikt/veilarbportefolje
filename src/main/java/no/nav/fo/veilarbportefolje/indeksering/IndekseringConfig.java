package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.fo.veilarbportefolje.service.VeilederService;
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
import java.util.UUID;

import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY;
import static no.nav.fo.veilarbportefolje.util.PingUtils.ping;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class IndekseringConfig {

    private static final String URL = getRequiredProperty(VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY);

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public SolrClient solrClientSlave() {
        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(URL)
                .withConnectionTimeout(2000)
                .withSocketTimeout(10000)
                .withHttpClient(createHttpClientForSolr())
                .build();
    }

    @Bean
    public SolrClient solrClientMaster() {
        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(getRequiredProperty(VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY))
                .withConnectionTimeout(2000)
                .withSocketTimeout(10000)
                .withHttpClient(createHttpClientForSolr())
                .build();
    }

    @Bean
    public IndekseringService solrService(
            SolrClient solrClientMaster,
            SolrClient solrClientSlave,
            BrukerRepository brukerRepository,
            AktoerService aktoerService,
            AktivitetDAO aktivitetDAO,
            VeilederService veilederService,
            LockingTaskExecutor lockingTaskExecutor) {
        return new SolrService(solrClientMaster, solrClientSlave, brukerRepository, aktoerService, veilederService, aktivitetDAO, lockingTaskExecutor);
    }

    @Bean
    public Pingable solrServerPing() {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "HTTP via " + URL,
                "Solr-indeks for portefolje",
                true
        );
        return () -> ping(this::doPing, metadata);
    }

    @SneakyThrows
    private void doPing() {
        solrClientSlave().ping();
    }

    private HttpClient createHttpClientForSolr() {
        String username = getRequiredProperty(SYSTEMUSER_USERNAME);
        String password = getRequiredProperty(SYSTEMUSER_PASSWORD);
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
