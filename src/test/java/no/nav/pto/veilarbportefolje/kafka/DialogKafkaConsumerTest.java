package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.TestUtil.createUnleashMock;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC;
import static org.mockito.Mockito.mock;

public class DialogKafkaConsumerTest extends IntegrationTest {

    private static DialogService dialogService;
    private static JdbcTemplate jdbcTemplate;
    private static String indexName;


    @BeforeClass
    public static void beforeClass() {

        DataSource dataSource = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        BrukerRepository brukerRepository = new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate);
        indexName = generateId();
        dialogService = new DialogService(new DialogRepository(jdbcTemplate), new ElasticIndexer(mock(AktivitetDAO.class), brukerRepository, ELASTIC_CLIENT, mock(UnleashService.class), mock(MetricsClient.class), indexName));


        new KafkaConsumerRunnable<>(
                dialogService,
                createUnleashMock(),
                getKafkaConsumerProperties(),
                KAFKA_AKTIVITER_CONSUMER_TOPIC,
                ""
        );
    }


    private void createDialogDocument(Fnr fnr) {
        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("venterpasvarfrabruker", (String) null)
                .put("venterpasvarfranav", (String) null)
                .toString();

        createDocument(indexName, fnr, document);
    }
}
