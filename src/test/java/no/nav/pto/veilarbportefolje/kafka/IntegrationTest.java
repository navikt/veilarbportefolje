package no.nav.pto.veilarbportefolje.kafka;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONObject;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static no.nav.pto.veilarbportefolje.elastic.Constant.ELASTICSEARCH_VERSION;
import static no.nav.pto.veilarbportefolje.util.CollectionUtils.listOf;
import static org.apache.http.HttpHost.create;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.client.RestClient.builder;

public class IntegrationTest {
    protected final static ElasticsearchContainer ELASTICSEARCH_CONTAINER;
    protected final static RestHighLevelClient ELASTIC_CLIENT;

    protected final static KafkaContainer KAFKA_CONTAINER;
    protected final static Producer<String, String> KAFKA_PRODUCER;

    static {
        System.setProperty("NAIS_NAMESPACE", "test");
        ELASTICSEARCH_CONTAINER = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION);
        ELASTICSEARCH_CONTAINER.start();
        ELASTIC_CLIENT = new RestHighLevelClient(builder(create(ELASTICSEARCH_CONTAINER.getHttpHostAddress())));

        KAFKA_CONTAINER = new KafkaContainer();
        KAFKA_CONTAINER.start();

        HashMap<String, Object> produdcerConfig = new HashMap<>();
        produdcerConfig.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        produdcerConfig.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        produdcerConfig.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KAFKA_PRODUCER = new KafkaProducer<>(produdcerConfig);

    }

    protected static Properties getKafkaConsumerProperties() {
        Properties properties = new Properties();
        properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        properties.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(GROUP_ID_CONFIG, "veilarbportefolje-consumer");
        properties.put(MAX_POLL_RECORDS_CONFIG, 5);
        properties.put(SESSION_TIMEOUT_MS_CONFIG, 20000);
        properties.put(ENABLE_AUTO_COMMIT_CONFIG, true);
        properties.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    @SneakyThrows
    protected static GetResponse fetchDocument(String indexName, Fnr fnr) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(indexName);
        getRequest.id(fnr.toString());
        return ELASTIC_CLIENT.get(getRequest, DEFAULT);
    }

    @SneakyThrows
    protected static IndexResponse createDocument(String indexName, Fnr fnr, String json) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName);
        indexRequest.type("_doc");
        indexRequest.id(fnr.getFnr());
        indexRequest.source(json, XContentType.JSON);
        return ELASTIC_CLIENT.index(indexRequest, DEFAULT);
    }

    @SneakyThrows
    public static AcknowledgedResponse deleteIndex(String indexName) {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
        return ELASTIC_CLIENT.indices().delete(deleteIndexRequest, DEFAULT);
    }

    @SneakyThrows
    public static void createIndex(String indexName) {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        ELASTIC_CLIENT.indices().create(createIndexRequest, DEFAULT);
    }


    @SneakyThrows
    public static int countDocuments(String indexName) {
        Request request = new Request("GET", indexName + "/_count");
        Response response = ELASTIC_CLIENT.getLowLevelClient().performRequest(request);
        String entity = EntityUtils.toString(response.getEntity());
        return new JSONObject(entity).getInt("count");
    }



    public static void populateKafkaTopic(String topic, String key ,String payload) {

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);

        try {
            KAFKA_PRODUCER.send(record).get();
        } catch (InterruptedException | ExecutionException ignored) {
            throw new RuntimeException();
        }
    }


    public static void pollUntilHarOppdatertIElastic(Supplier<Boolean> func) {
        long t0 = currentTimeMillis();

        while (func.get()) {
            if (timeout(t0)) {
                throw new RuntimeException();
            }
        }
    }

    private static boolean timeout(long t0) {
        return currentTimeMillis() - t0 > 12_000;
    }

    public static void skrivBrukereTilTestindeks(String indexName, ElasticIndexer elasticIndexer, List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(indexName, elasticIndexer, brukere.toArray(array));
    }

    @SneakyThrows
    private static void skrivBrukereTilTestindeks(String indexName, ElasticIndexer elasticIndexer, OppfolgingsBruker... brukere) {
        elasticIndexer.skrivTilIndeks(indexName, listOf(brukere));
        ELASTIC_CLIENT.indices().refreshAsync(new RefreshRequest(indexName), RequestOptions.DEFAULT, new ActionListener<RefreshResponse>() {
            @Override
            public void onResponse(RefreshResponse refreshResponse) {
                System.out.println("refreshed");
            }

            @Override
            public void onFailure(Exception e) {
                System.err.println("noe gikk galt her " + e);
            }
        });
    }
}
