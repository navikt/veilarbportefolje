package no.nav.pto.veilarbportefolje.cv;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.HashMap;
import java.util.Properties;

import static no.nav.pto.veilarbportefolje.elastic.Constant.ELASTICSEARCH_VERSION;
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

}
