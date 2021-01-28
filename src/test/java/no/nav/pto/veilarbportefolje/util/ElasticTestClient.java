package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.IndexName;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static no.nav.common.json.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ElasticTestClient {

    private final RestHighLevelClient restHighLevelClient;
    private final IndexName indexName;

    @Autowired
    public ElasticTestClient(RestHighLevelClient restHighLevelClient, IndexName indexName) {
        this.restHighLevelClient = restHighLevelClient;
        this.indexName = indexName;
    }

    public OppfolgingsBruker hentBrukerFraElastic(AktorId aktoerId) {
        return getDocument(aktoerId)
                .map(resp -> JsonUtils.fromJson(resp.getSourceAsString(), OppfolgingsBruker.class))
                .orElseThrow();
    }

    @SneakyThrows
    public GetResponse fetchDocument(AktorId aktoerId) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(indexName.getValue());
        getRequest.id(aktoerId.toString());
        return restHighLevelClient.get(getRequest, DEFAULT);
    }

    @SneakyThrows
    public IndexResponse createDocument(AktorId aktoerId, String json) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName.getValue());
        indexRequest.type("_doc");
        indexRequest.id(aktoerId.toString());
        indexRequest.source(json, XContentType.JSON);
        return restHighLevelClient.index(indexRequest, DEFAULT);
    }

    @SneakyThrows
    public AcknowledgedResponse deleteIndex(IndexName indexName) {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName.getValue());
        return restHighLevelClient.indices().delete(deleteIndexRequest, DEFAULT);
    }

    @SneakyThrows
    public void createIndex() {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName.getValue());
        restHighLevelClient.indices().create(createIndexRequest, DEFAULT);
    }


    @SneakyThrows
    public int countDocuments() {
        Request request = new Request("GET", indexName.getValue() + "/_count");
        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String entity = EntityUtils.toString(response.getEntity());
        return new JSONObject(entity).getInt("count");
    }

    public void createUserInElastic(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("oppfolging", true)
                .toString();

        //create document
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName.getValue());
        indexRequest.type("_doc");
        indexRequest.id(aktoerId.toString());
        indexRequest.source(document, XContentType.JSON);

        try {
            restHighLevelClient.index(indexRequest, DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Optional<GetResponse> getResponse = getDocument(aktoerId);

        assertThat(getResponse).isPresent();
        assertThat(getResponse.get().isExists()).isTrue();
    }

    public void createUserInElastic(OppfolgingsBruker bruker) {
        //create document
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName.getValue());
        indexRequest.type("_doc");
        indexRequest.id(bruker.getAktoer_id());
        indexRequest.source(toJson(bruker), XContentType.JSON);

        try {
            restHighLevelClient.index(indexRequest, DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final AktorId aktoerId = AktorId.of(bruker.getAktoer_id());
        final Optional<GetResponse> getResponse = getDocument(aktoerId);

        assertThat(getResponse).isPresent();
        assertThat(getResponse.get().isExists()).isTrue();
    }

    public Optional<GetResponse> getDocument(AktorId aktoerId) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(indexName.getValue());
        getRequest.id(aktoerId.toString());
        try {
            return Optional.of(restHighLevelClient.get(getRequest, DEFAULT));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static void pollElasticUntil(Supplier<Boolean> func) {
        long t0 = currentTimeMillis();

        while (!func.get()) {
            if (timeout(t0)) {
                throw new RuntimeException();
            }
        }
    }

    private static boolean timeout(long t0) {
        return currentTimeMillis() - t0 > 12_000;
    }

    @SneakyThrows
    public void oppdaterArbeidsliste(AktorId aktoerId, boolean isAktiv) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv", isAktiv)
                .endObject();

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indexName.getValue());
        updateRequest.type("_doc");
        updateRequest.id(aktoerId.toString());
        updateRequest.doc(content);

        restHighLevelClient.update(updateRequest, DEFAULT);
    }
}
