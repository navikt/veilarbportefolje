package no.nav.pto.veilarbportefolje.config;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.IdUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV1;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.dialog.DialogRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolginsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.*;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepository;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakRepository;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.cv.CvRepository;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.dialog.DialogRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.elastic.IndexName;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerRunnable;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.mock.MetricsClientMock;
import no.nav.pto.veilarbportefolje.oppfolging.*;
import no.nav.pto.veilarbportefolje.persononinfo.PersonRepository;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepository;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepositoryV2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Properties;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.elastic.Constant.ELASTICSEARCH_VERSION;
import static org.apache.http.HttpHost.create;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.elasticsearch.client.RestClient.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        FilmottakConfig.class,
        BrukerService.class,
        RegistreringService.class,
        RegistreringRepository.class,
        AktivitetService.class,
        PersistentOppdatering.class,
        OppfolgingAvsluttetService.class,
        ElasticService.class,
        ElasticServiceV2.class,
        TiltakRepository.class,
        AktivitetDAO.class,
        BrukerRepository.class,
        OppfolgingRepository.class,
        ManuellStatusService.class,
        DialogService.class,
        DialogRepository.class,
        ElasticIndexer.class,
        CvRepository.class,
        CvService.class,
        RegistreringRepository.class,
        PersonRepository.class,
        NyForVeilederService.class,
        VeilederTilordnetService.class,
        OppfolgingStartetService.class,
        SisteEndringService.class,
        SisteEndringRepository.class,
        SistLestService.class,
        MalService.class,
        OppfolgingService.class,
        ArbeidslisteRepositoryV1.class,
        ArbeidslisteRepositoryV2.class
})
public class ApplicationConfigTest {

    private static final ElasticsearchContainer ELASTICSEARCH_CONTAINER;
    private static final KafkaContainer KAFKA_CONTAINER;

    static {
        ELASTICSEARCH_CONTAINER = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION);
        ELASTICSEARCH_CONTAINER.start();

        System.setProperty("elastic.indexname", IdUtils.generateId());
        System.setProperty("elastic.httphostaddress", ELASTICSEARCH_CONTAINER.getHttpHostAddress());

        KAFKA_CONTAINER = new KafkaContainer();
        KAFKA_CONTAINER.start();

        System.setProperty("NAIS_NAMESPACE", "localhost");
        System.setProperty("KAFKA_BROKERS_URL", KAFKA_CONTAINER.getBootstrapServers());
    }

    public static Properties kafkaConsumerProperties() {
        Properties properties = new Properties();
        properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, System.getProperty("KAFKA_BROKERS_URL"));
        properties.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(GROUP_ID_CONFIG, "veilarbportefolje-consumer");
        properties.put(MAX_POLL_RECORDS_CONFIG, 5);
        properties.put(SESSION_TIMEOUT_MS_CONFIG, 20000);
        properties.put(ENABLE_AUTO_COMMIT_CONFIG, true);
        properties.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaCvConsumer(CvService cvService, UnleashService unleashService, MetricsClient metricsClient){
        return new KafkaConsumerRunnable<>(
                cvService,
                unleashService,
                ApplicationConfigTest.kafkaConsumerProperties(),
                KafkaConfig.Topic.PAM_SAMTYKKE_ENDRET_V1,
                metricsClient
        );
    }

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, System.getProperty("KAFKA_BROKERS_URL"));
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(props);
    }

    @Bean
    public TestDataClient dbTestClient(JdbcTemplate jdbcTemplate, ElasticTestClient elasticTestClient) {
        return new TestDataClient(jdbcTemplate, elasticTestClient);
    }

    @Bean
    public ElasticTestClient elasticTestClient(RestHighLevelClient restHighLevelClient, IndexName indexName) {
        return new ElasticTestClient(restHighLevelClient, indexName);
    }

    @Bean
    public VedtakstottePilotRequest vedtakstottePilotRequest(){
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
    public UnleashService unleashService() {
        final UnleashService mock = mock(UnleashService.class);
        when(mock.isEnabled(anyString())).thenReturn(true);
        return mock;
    }

    @Bean
    public MetricsClient metricsClient() { return new MetricsClientMock(); }

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
        final String httpHostAddress = System.getProperty("elastic.httphostaddress");
        return new RestHighLevelClient(builder(create(httpHostAddress)));
    }

    @Bean
    public VeilarbVeilederClient veilarbVeilederClient() {
        return mock(VeilarbVeilederClient.class);
    }

    @Bean
    public OppfolgingRepositoryV2 oppfolgingRepositoryV2() {
        return mock(OppfolgingRepositoryV2.class);
    }

    @Bean
    public OppfolginsbrukerRepositoryV2 oppfolginsbrukerRepositoryV2() {
        return mock(OppfolginsbrukerRepositoryV2.class);
    }

    @Bean
    public DialogRepositoryV2 dialogRepositoryV2(){
        return mock(DialogRepositoryV2.class);
    }

    @Bean
    public VedtakStatusRepositoryV2 vedtakStatusRepositoryV2(){
        return  mock(VedtakStatusRepositoryV2.class);
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(){
        return mock(SystemUserTokenProvider.class);
    }

    @Bean
    @Primary
    @Autowired
    ArbeidslisteService arbeidslisteServiceOracle(AktorClient aktorClient, ArbeidslisteRepositoryV1 arbeidslisteRepository,
                                                  BrukerService brukerService, ElasticServiceV2 elasticServiceV2,
                                                  MetricsClient metricsClient) {
        return new ArbeidslisteService(aktorClient, arbeidslisteRepository, brukerService, elasticServiceV2, metricsClient);
    }

    @Bean("PostgresArbeidslisteService")
    @Autowired
    ArbeidslisteService arbeidslisteService(AktorClient aktorClient, ArbeidslisteRepositoryV2 arbeidslisteRepository,
                                            BrukerService brukerService,
                                            ElasticServiceV2 elasticServiceV2, MetricsClient metricsClient) {
        return new ArbeidslisteService(aktorClient, arbeidslisteRepository, brukerService, elasticServiceV2, metricsClient);
    }

    @Bean(name="PostgresJdbc")
    public JdbcTemplate db() {
        return SingletonPostgresContainer.init().createJdbcTemplate();
    }

}
