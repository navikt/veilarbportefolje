package no.nav.fo.solr;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import no.nav.fo.service.SolrServiceImpl;
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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;
import static no.nav.dialogarena.smoketest.Tag.SMOKETEST;
import static no.nav.fo.config.LocalJndiContextConfig.setupDataSourceWithCredentials;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.fo.util.AktivitetUtils.applyAktivitetstatusToDocument;
import static no.nav.fo.util.DateUtils.toIsoUTC;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag(SMOKETEST)
@SuppressWarnings("Duplicates")
public class SolrUpdateSmoketest {
    private static SolrClient solrClient;
    private static String VEILARBPORTEFOLJE = "veilarbportefolje";

    private static final PersonId AREMARK_PERSON_ID = PersonId.of("3125339");
    private static final AktoerId AREMARK_AKTOER_ID = AktoerId.of("1000096233942");
    private static DataSource ds;
    private static JdbcTemplate jdbcTemplate;
    private static NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static BrukerRepository brukerRepository;
    private static SolrService solrService;

    @BeforeEach
    public void setup() {
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(VEILARBPORTEFOLJE));
        Properties properties = FasitUtils.getApplicationEnvironment(VEILARBPORTEFOLJE);
        solrClient = new HttpSolrClient.Builder()
                    .withBaseSolrUrl(properties.getProperty("veilarbportefolje.solr.masternode"))
                    .withHttpClient(createHttpClientForSolr())
                    .build();

        ds =  setupDataSourceWithCredentials(FasitUtils.getDbCredentials(VEILARBPORTEFOLJE));
        jdbcTemplate = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        brukerRepository = new BrukerRepository(jdbcTemplate, ds, namedParameterJdbcTemplate);
        solrService = new SolrServiceImpl(solrClient, solrClient,brukerRepository,
                mock(ArbeidslisteRepository.class),mock(AktoerService.class), mock(AktivitetDAO.class));
        }

    @Test
    @SneakyThrows
    public void skalOppdatereSolrIndeksKorrekt() {
        SolrInputDocument solrInputDocument = brukerRepository.retrieveBrukeremedBrukerdata(asList(AREMARK_PERSON_ID)).get(0);
        applyAktivitetstatusToDocument(solrInputDocument,getAktivitetStatusesWithOffset(0));

        solrClient.add(solrInputDocument);
        solrClient.commit();

        SolrQuery solrQuery = new SolrQuery("person_id:"+AREMARK_PERSON_ID.toString());
        QueryResponse response = solrClient.query(solrQuery);
        SolrDocument solrDocument = response.getResults().get(0);
        Bruker bruker = Bruker.of(solrDocument);
        assertThat(solrDocument.getFieldNames()).containsAll(solrDocument.getFieldNames());
        assertThat(bruker.getAktiviteter().size()).isEqualTo(10);
        solrService.slettBruker(AREMARK_PERSON_ID);
        solrService.commit();
        QueryResponse responseSlettet = solrClient.query(solrQuery);
        assertThat(responseSlettet.getResults()).isEmpty();
    }

    @Test
    @SneakyThrows
    public void skalSorterePaaUtlopsdato() {
        Timestamp fodselsdato1 = timestampMinusYears(20);
        Timestamp fodselsdato2 = timestampMinusYears(40);
        Timestamp fodselsdato3 = timestampMinusYears(60);
        SolrInputDocument solrInputDocument1 = getBaseDocument("1111", fodselsdato1);
        SolrInputDocument solrInputDocument2 = getBaseDocument("2222", fodselsdato2);
        SolrInputDocument solrInputDocument3 = getBaseDocument("3333", fodselsdato3);
        solrClient.add(solrInputDocument1);
        solrClient.add(solrInputDocument2);
        solrClient.add(solrInputDocument3);
        solrClient.commit();
        BrukereMedAntall brukereDescending = solrService.hentBrukere("testenhet", Optional.empty(), "descending", "fodselsnummer", new Filtervalg(), null, null);
        BrukereMedAntall brukereAscending = solrService.hentBrukere("testenhet", Optional.empty(), "ascending", "fodselsnummer", new Filtervalg(), null, null);
        List<LocalDateTime> ascending = brukereAscending.getBrukere().stream().map(Bruker::getFodselsdato).collect(Collectors.toList());
        List<LocalDateTime> descending = brukereDescending.getBrukere().stream().map(Bruker::getFodselsdato).collect(Collectors.toList());
        assertThat(ascending).isEqualTo(asList(fodselsdato3.toLocalDateTime(), fodselsdato2.toLocalDateTime(),fodselsdato1.toLocalDateTime()));
        assertThat(descending).isEqualTo(asList(fodselsdato1.toLocalDateTime(), fodselsdato2.toLocalDateTime(),fodselsdato3.toLocalDateTime()));
        solrService.slettBrukere(asList(PersonId.of("1111"),PersonId.of("2222"), PersonId.of("3333")));
    }

    @Test
    @SneakyThrows
    public void skalSorterePaAktiviteter() {
        Timestamp fodselsdato1 = timestampMinusYears(20);
        Timestamp fodselsdato2 = timestampMinusYears(40);
        SolrInputDocument solrInputDocument1 = getBaseDocument("1111", fodselsdato1);
        SolrInputDocument solrInputDocument2 = getBaseDocument("2222", fodselsdato2);
        applyAktivitetstatusToDocument(solrInputDocument1, getAktivitetStatusesWithOffset(0));
        applyAktivitetstatusToDocument(solrInputDocument2, getAktivitetStatusesWithOffset(10));
        solrClient.add(solrInputDocument1);
        solrClient.add(solrInputDocument2);
        solrClient.commit();
        BrukereMedAntall brukereAscending = solrService.hentBrukere("testenhet",Optional.empty(), "ascending", "valgteaktiviteter", filterMedAlleAktiviteter(), null, null);
        BrukereMedAntall brukereDescending = solrService.hentBrukere("testenhet",Optional.empty(), "descending", "valgteaktiviteter", filterMedAlleAktiviteter(), null, null);
        List<Map<String, Timestamp>> aktiviteterAscending = brukereAscending.getBrukere().stream().map(Bruker::getAktiviteter).collect(Collectors.toList());
        List<Map<String, Timestamp>> aktiviteterDescending = brukereDescending.getBrukere().stream().map(Bruker::getAktiviteter).collect(Collectors.toList());
        Timestamp tiltakAscending = aktiviteterAscending.get(0).get("tiltak");
        Timestamp tiltakDescending = aktiviteterDescending.get(0).get("tiltak");
        assertThat(tiltakAscending).isBefore(tiltakDescending);
    }

    private Set<AktivitetStatus> getAktivitetStatusesWithOffset(int offset) {
        Set<AktivitetStatus> statuser = new HashSet<>();
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"tiltak", true, timestampPlusYears(1+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"behandling", true, timestampPlusYears(2+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"sokeavtale", true, timestampPlusYears(3+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"stilling", true, timestampPlusYears(4+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"ijobb", true, timestampPlusYears(5+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"samtalereferat", true, timestampPlusYears(6+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"egen", true, timestampPlusYears(7+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"gruppeaktivitet", true, timestampPlusYears(8+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"mote", true, timestampPlusYears(9+offset)));
        statuser.add(AktivitetStatus.of(AREMARK_PERSON_ID,AREMARK_AKTOER_ID,"utdanningaktivitet", true, timestampPlusYears(10+offset)));
        return statuser;
    }

    private static Filtervalg filterMedAlleAktiviteter() {
        Map<String, AktivitetFiltervalg> aktiviteter = new HashMap<>();
        aktivitetTyperList.forEach( type -> aktiviteter.put(type.name(), AktivitetFiltervalg.JA));
        return new Filtervalg().setAktiviteter(aktiviteter);
    }

    private static Timestamp timestampPlusYears(int years) {
        return  new Timestamp(LocalDateTime.now().plusYears(years).toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    private static Timestamp timestampMinusYears(int years) {
        return  new Timestamp(LocalDateTime.now().minusYears(years).toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    private static SolrInputDocument getBaseDocument(String personid, Timestamp fodselsdato) {
        SolrInputDocument document = new SolrInputDocument();
        document.setField("person_id", personid);
        document.setField("fnr", "10109012345");
        document.setField("etternavn", "etternavn");
        document.setField("fornavn", "fornavn");
        document.setField("formidlingsgruppekode", "formidlingsgruppekode");
        document.setField("kvalifiseringsgruppekode", "kvalifiseringsgruppekode");
        document.setField("rettighetsgruppekode", "rettighetsgruppekode");
        document.setField("egen_ansatt", false);
        document.setField("er_doed", false);
        document.setField("fodselsdag_i_mnd", 10);
        document.setField("fodselsdato", toIsoUTC(fodselsdato));
        document.setField("kjonn", "M");
        document.setField("enhet_id", "testenhet");
        document.setField("oppfolging", true);
        return document;
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
