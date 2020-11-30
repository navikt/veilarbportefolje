package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;

import java.nio.charset.StandardCharsets;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Slf4j
public class ElasticTestUtils {

    @SneakyThrows
    public static void opprettNyIndeks(String navn, RestHighLevelClient restHighLevelClient) {
        String json = IOUtils.toString(ElasticTestUtils.class.getResource("/elastic_settings.json"), StandardCharsets.UTF_8);

        CreateIndexRequest request = new CreateIndexRequest(navn)
                .source(json, XContentType.JSON);

        CreateIndexResponse response = restHighLevelClient.indices().create(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke opprette ny indeks {}", navn);
            throw new RuntimeException();
        }
    }

}
