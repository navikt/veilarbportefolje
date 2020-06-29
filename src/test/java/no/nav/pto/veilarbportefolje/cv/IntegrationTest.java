package no.nav.pto.veilarbportefolje.cv;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import javax.sql.DataSource;

import static no.nav.pto.veilarbportefolje.elastic.Constant.ELASTICSEARCH_VERSION;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;
import static org.apache.http.HttpHost.create;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.client.RestClient.builder;

public class IntegrationTest {
    protected final static ElasticsearchContainer ELASTICSEARCH_CONTAINER;

    static {
        ELASTICSEARCH_CONTAINER = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION);
        ELASTICSEARCH_CONTAINER.start();
    }

    static RestHighLevelClient elasticClient;
    static JdbcTemplate jdbcTemplate;

    @BeforeClass
    public static void beforeClass() {
        DataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        elasticClient = new RestHighLevelClient(builder(create(ELASTICSEARCH_CONTAINER.getHttpHostAddress())));
    }

    @Before
    public void setUp() {
        createIndex();
    }

    @After
    public void tearDown() {
        deleteIndex();
        truncateAllTablesExceptFlywayTable();
    }


    @SneakyThrows
    static GetResponse fetchDocument(Fnr fnr) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(getAlias());
        getRequest.id(fnr.toString());
        return elasticClient.get(getRequest, DEFAULT);
    }

    @SneakyThrows
    static IndexResponse createDocument(Fnr fnr, String json) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(getAlias());
        indexRequest.type("_doc");
        indexRequest.id(fnr.getFnr());
        indexRequest.source(json, XContentType.JSON);
        return elasticClient.index(indexRequest, DEFAULT);
    }

    @SneakyThrows
    private static AcknowledgedResponse deleteIndex() {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(getAlias());
        return elasticClient.indices().delete(deleteIndexRequest, DEFAULT);
    }

    @SneakyThrows
    private static void createIndex() {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(getAlias());
        elasticClient.indices().create(createIndexRequest, DEFAULT);
    }

    private void truncateAllTablesExceptFlywayTable() {
        jdbcTemplate.execute("TRUNCATE TABLE AKTIVITETER");
        jdbcTemplate.execute("TRUNCATE TABLE AKTOERID_TO_PERSONID");
        jdbcTemplate.execute("TRUNCATE TABLE ARBEIDSLISTE");
        jdbcTemplate.execute("TRUNCATE TABLE BRUKERSTATUS_AKTIVITETER");
        jdbcTemplate.execute("TRUNCATE TABLE BRUKERTILTAK");
        jdbcTemplate.execute("TRUNCATE TABLE BRUKER_DATA");
        jdbcTemplate.execute("TRUNCATE TABLE BRUKER_PROFILERING");
        jdbcTemplate.execute("TRUNCATE TABLE BRUKER_REGISTRERING");
        jdbcTemplate.execute("TRUNCATE TABLE DIALOG");
        jdbcTemplate.execute("TRUNCATE TABLE ENHETTILTAK");
        jdbcTemplate.execute("TRUNCATE TABLE KRR");
        jdbcTemplate.execute("TRUNCATE TABLE METADATA");
        jdbcTemplate.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        jdbcTemplate.execute("TRUNCATE TABLE OPPFOLGING_DATA");
        jdbcTemplate.execute("TRUNCATE TABLE VEDTAKSTATUS_DATA");
    }
}
