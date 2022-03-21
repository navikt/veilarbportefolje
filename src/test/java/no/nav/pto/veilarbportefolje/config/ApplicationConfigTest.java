package no.nav.pto.veilarbportefolje.config;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.UtdanningsAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.*;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.cv.CVRepositoryV2;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.database.BrukerDataRepository;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogRepositoryV2;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.mock.MetricsClientMock;
import no.nav.pto.veilarbportefolje.opensearch.*;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import no.nav.pto.veilarbportefolje.oppfolging.*;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.persononinfo.PersonRepository;
import no.nav.pto.veilarbportefolje.postgres.opensearch.AktivitetOpensearchService;
import no.nav.pto.veilarbportefolje.postgres.opensearch.AktoerDataOpensearchMapper;
import no.nav.pto.veilarbportefolje.postgres.opensearch.PostgresOpensearchMapper;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepositoryV2;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepositoryV2;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.*;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepositoryV2;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchUtils.createClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        ArbeidslisteService.class,
        BrukerService.class,
        RegistreringService.class,
        AktivitetService.class,
        OppfolgingAvsluttetService.class,
        OpensearchService.class,
        OpensearchIndexer.class,
        OpensearchIndexerV2.class,
        OpensearchAdminService.class,
        HovedIndekserer.class,
        AktiviteterRepositoryV2.class,
        BrukerRepository.class,
        OppfolgingRepository.class,
        OppfolgingRepositoryV2.class,
        OppfolgingsbrukerRepositoryV2.class,
        ManuellStatusService.class,
        DialogService.class,
        DialogRepository.class,
        DialogRepositoryV2.class,
        CVRepositoryV2.class,
        CVService.class,
        RegistreringRepositoryV2.class,
        PersonRepository.class,
        NyForVeilederService.class,
        VeilederTilordnetService.class,
        OppfolgingStartetService.class,
        SisteEndringService.class,
        SisteEndringRepository.class,
        SisteEndringRepositoryV2.class,
        SistLestService.class,
        MalService.class,
        OppfolgingService.class,
        ArbeidslisteRepositoryV2.class,
        UtdanningsAktivitetService.class,
        ArenaHendelseRepository.class,
        GruppeAktivitetRepositoryV2.class,
        TiltakRepositoryV2.class,
        BrukerDataService.class,
        BrukerDataRepository.class,
        PostgresOpensearchMapper.class,
        AktoerDataOpensearchMapper.class,
        AktivitetOpensearchService.class,
        YtelsesService.class,
        YtelsesServicePostgres.class,
        YtelsesRepository.class,
        YtelsesRepositoryV2.class,
        YtelsesStatusRepositoryV2.class,
        OppfolgingPeriodeService.class
})
public class ApplicationConfigTest {

    private static final OpenSearchContainer OPENSEARCH_CONTAINER;
    private static final String OPENSEARCH_VERSION = "1.2.3";
    private static final String OPENSEARCH_TEST_PASSWORD = "test";
    private static final String OPENSEARCH_TEST_USERNAME = "opensearch";

    static {
        OPENSEARCH_CONTAINER = new OpenSearchContainer(OPENSEARCH_VERSION);
        OPENSEARCH_CONTAINER.start();
    }


    @Bean
    public TestDataClient dbTestClient(JdbcTemplate jdbcTemplate, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplatePostgres, OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2, ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2, RegistreringRepositoryV2 registreringRepositoryV2, OpensearchTestClient opensearchTestClient, OppfolgingRepositoryV2 oppfolgingRepositoryV2) {
        return new TestDataClient(jdbcTemplate, jdbcTemplatePostgres, registreringRepositoryV2, oppfolgingsbrukerRepositoryV2, arbeidslisteRepositoryV2, opensearchTestClient, oppfolgingRepositoryV2);
    }

    @Bean
    public OpensearchTestClient opensearchTestClient(RestHighLevelClient restHighLevelClient, IndexName indexName) {
        return new OpensearchTestClient(restHighLevelClient, indexName);
    }

    @Bean
    public VedtakstottePilotRequest vedtakstottePilotRequest() {
        VedtakstottePilotRequest vedtakstottePilotRequest = mock(VedtakstottePilotRequest.class);
        when(vedtakstottePilotRequest.erVedtakstottePilotPa(any())).thenReturn(true);
        return vedtakstottePilotRequest;
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }

    @Bean
    public IndexName indexName() {
        return new IndexName(generateId());
    }

    @Bean
    public AktorClient aktorClient() {
        return mock(AktorClient.class);
    }

    @Bean
    public AktorClient aktorClientSystem() {
        return mock(AktorClient.class);
    }

    @Bean
    public UnleashService unleashService() {
        final UnleashService mock = mock(UnleashService.class);
        when(mock.isEnabled(anyString())).thenReturn(true);
        return mock;
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    @Primary
    public DataSource hsqldbDataSource() {
        return TestUtil.setupInMemoryDatabase();
    }


    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        return createClient(opensearchClientConfig());
    }

    @Bean
    public OpensearchClientConfig opensearchClientConfig() {
        return OpensearchClientConfig.builder()
                .username(OPENSEARCH_TEST_USERNAME)
                .password(OPENSEARCH_TEST_PASSWORD)
                .hostname(OPENSEARCH_CONTAINER.getHost())
                .port(OPENSEARCH_CONTAINER.getFirstMappedPort())
                .scheme("http")
                .build();
    }

    @Bean
    public VeilarbVeilederClient veilarbVeilederClient() {
        return mock(VeilarbVeilederClient.class);
    }

    @Bean
    public VedtakStatusRepositoryV2 vedtakStatusRepositoryV2() {
        return mock(VedtakStatusRepositoryV2.class);
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        return mock(SystemUserTokenProvider.class);
    }

    @Bean(name = "PostgresJdbc")
    public JdbcTemplate db() {
        return SingletonPostgresContainer.init().createJdbcTemplate();
    }

    @Bean("PostgresJdbcReadOnly")
    public JdbcTemplate dbMockReadOnly() {
        return db();
    }

    @Bean(name = "PostgresNamedJdbcReadOnly")
    public NamedParameterJdbcTemplate dbNamedMockReadOnly() {
        return new NamedParameterJdbcTemplate(db());
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }


}
