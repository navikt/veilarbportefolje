package no.nav.pto.veilarbportefolje.config;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.UtdanningsAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.cv.CVRepositoryV2;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.dialog.DialogRepositoryV2;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkClient;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.mock.MetricsClientMock;
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer;
import no.nav.pto.veilarbportefolje.opensearch.IndexName;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchCountService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import no.nav.pto.veilarbportefolje.oppfolging.ManuellStatusService;
import no.nav.pto.veilarbportefolje.oppfolging.NyForVeilederService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingAvsluttetService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingStartetService;
import no.nav.pto.veilarbportefolje.oppfolging.SkjermingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.SkjermingService;
import no.nav.pto.veilarbportefolje.oppfolging.VeilederTilordnetService;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.PersonOpprinnelseRepository;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.PersonOpprinnelseService;
import no.nav.pto.veilarbportefolje.postgres.AktivitetOpensearchService;
import no.nav.pto.veilarbportefolje.postgres.BrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepositoryV2;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Avvik14aVedtakService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepositoryV2;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.pto.veilarbportefolje.util.TestDataClient;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import no.nav.pto.veilarbportefolje.vedtakstotte.Utkast14aStatusRepository;
import org.opensearch.client.RestHighLevelClient;
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
import java.util.List;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.domene.Kjonn.K;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchUtils.createClient;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        Siste14aVedtakRepository.class,
        Siste14aVedtakService.class,
        ArbeidslisteService.class,
        BrukerServiceV2.class,
        BrukerRepositoryV2.class,
        RegistreringService.class,
        AktivitetService.class,
        OppfolgingAvsluttetService.class,
        OpensearchService.class,
        OpensearchIndexer.class,
        OpensearchIndexerV2.class,
        OpensearchAdminService.class,
        HovedIndekserer.class,
        AktiviteterRepositoryV2.class,
        OppfolgingRepositoryV2.class,
        OppfolgingsbrukerRepositoryV3.class,
        OppfolgingsbrukerServiceV2.class,
        ManuellStatusService.class,
        DialogService.class,
        DialogRepositoryV2.class,
        CVRepositoryV2.class,
        CVService.class,
        RegistreringRepositoryV2.class,
        NyForVeilederService.class,
        VeilederTilordnetService.class,
        OppfolgingStartetService.class,
        SisteEndringService.class,
        SisteEndringRepositoryV2.class,
        SistLestService.class,
        MalService.class,
        OppfolgingService.class,
        ArbeidslisteRepositoryV2.class,
        UtdanningsAktivitetService.class,
        ArenaHendelseRepository.class,
        GruppeAktivitetRepositoryV2.class,
        TiltakRepositoryV2.class,
        PostgresOpensearchMapper.class,
        AktivitetOpensearchService.class,
        YtelsesService.class,
        YtelsesRepositoryV2.class,
        YtelsesStatusRepositoryV2.class,
        OppfolgingPeriodeService.class,
        SkjermingService.class,
        SkjermingRepository.class,
        PdlService.class,
        PdlIdentRepository.class,
        PdlPersonRepository.class,
        OpensearchCountService.class,
        KodeverkService.class,
        PersonOpprinnelseService.class,
        PersonOpprinnelseRepository.class,
        Avvik14aVedtakService.class
})
public class ApplicationConfigTest {

    private static final OpenSearchContainer OPENSEARCH_CONTAINER;
    private static final String OPENSEARCH_TEST_PASSWORD = "test";
    private static final String OPENSEARCH_TEST_USERNAME = "opensearch";

    static {
        OPENSEARCH_CONTAINER = new OpenSearchContainer();
        OPENSEARCH_CONTAINER.start();
    }


    @Bean
    public TestDataClient dbTestClient(JdbcTemplate jdbcTemplatePostgres,
                                       OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2,
                                       RegistreringRepositoryV2 registreringRepositoryV2, OpensearchTestClient opensearchTestClient,
                                       OppfolgingRepositoryV2 oppfolgingRepositoryV2, PdlIdentRepository pdlIdentRepository, PdlPersonRepository pdlPersonRepository) {
        return new TestDataClient(jdbcTemplatePostgres, registreringRepositoryV2, oppfolgingsbrukerRepository, arbeidslisteRepositoryV2, opensearchTestClient, oppfolgingRepositoryV2, pdlIdentRepository, pdlPersonRepository);
    }

    @Bean
    public OpensearchTestClient opensearchTestClient(RestHighLevelClient restHighLevelClient, OpensearchAdminService opensearchAdminService,
                                                     OpensearchCountService opensearchCountService, IndexName indexName) {
        return new OpensearchTestClient(restHighLevelClient, opensearchAdminService, opensearchCountService, indexName);
    }

    @Bean
    public VedtaksstotteClient vedtaksstotteClient() {
        VedtaksstotteClient vedtaksstotteClient = mock(VedtaksstotteClient.class);
        when(vedtaksstotteClient.erVedtakstottePilotPa(any())).thenReturn(true);
        return vedtaksstotteClient;
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
    public Utkast14aStatusRepository vedtakStatusRepositoryV2() {
        return mock(Utkast14aStatusRepository.class);
    }

    @Bean
    public DataSource dataSource() {
        return SingletonPostgresContainer.init().createDataSource();
    }

    @Bean
    @Primary
    public JdbcTemplate db(DataSource datasource) {
        return new JdbcTemplate(datasource);
    }

    @Bean("PostgresJdbcReadOnly")
    public JdbcTemplate dbReadOnly(DataSource datasource) {
        return new JdbcTemplate(datasource);
    }

    @Bean(name = "PostgresNamedJdbcReadOnly")
    public NamedParameterJdbcTemplate dbNamedReadOnly(DataSource datasource) {
        return new NamedParameterJdbcTemplate(datasource);
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager(DataSource datasource) {
        return new DataSourceTransactionManager(datasource);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return mock(AzureAdOnBehalfOfTokenClient.class);
    }

    @Bean
    public PdlPortefoljeClient pdlClient() {
        PdlPortefoljeClient pdlClient = mock(PdlPortefoljeClient.class);
        when(pdlClient.hentIdenterFraPdl(any())).thenReturn(List.of(
                new PDLIdent(randomFnr().get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(randomAktorId().get(), false, PDLIdent.Gruppe.AKTORID)
        ));
        when(pdlClient.hentBrukerDataFraPdl(any())).thenReturn(
                new PDLPerson().setKjonn(K));
        return pdlClient;
    }

    @Bean
    public KodeverkClient kodeverkClient() {
        return mock(KodeverkClient.class);
    }

    @Bean
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        return mock(AzureAdMachineToMachineTokenClient.class);
    }
}
