package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.IndexName;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static no.nav.common.json.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class OpensearchTestClient {

    private final RestHighLevelClient restHighLevelClient;
    private final IndexName indexName;

    @Autowired
    public OpensearchTestClient(RestHighLevelClient restHighLevelClient, IndexName indexName) {
        this.restHighLevelClient = restHighLevelClient;
        this.indexName = indexName;
    }

    public OppfolgingsBruker hentBrukerFraOpensearch(AktorId aktoerId) {
        return getDocument(aktoerId)
                .map(resp -> JsonUtils.fromJson(resp.getSourceAsString(), OppfolgingsBruker.class))
                .orElseThrow();
    }

    @SneakyThrows
    public GetResponse fetchDocument(AktorId aktoerId) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(indexName.getValue());
        getRequest.id(aktoerId.toString());
        return restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
    }

    @SneakyThrows
    public IndexResponse createDocument(AktorId aktoerId, String json) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName.getValue());
        indexRequest.id(aktoerId.toString());
        indexRequest.source(json, XContentType.JSON);
        return restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @SneakyThrows
    public int countDocuments() {
        Request request = new Request("GET", indexName.getValue() + "/_count");
        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String entity = EntityUtils.toString(response.getEntity());
        return new JSONObject(entity).getInt("count");
    }

    public void createUserInOpensearch(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("oppfolging", true)
                .toString();

        //create document
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName.getValue());
        indexRequest.id(aktoerId.toString());
        indexRequest.source(document, XContentType.JSON);

        try {
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Optional<GetResponse> getResponse = getDocument(aktoerId);

        assertThat(getResponse).isPresent();
        assertThat(getResponse.get()).isNotNull();
    }

    public void createUserInOpensearch(OppfolgingsBruker bruker) {
        //create document
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(indexName.getValue());
        indexRequest.id(bruker.getAktoer_id());
        indexRequest.source(toJson(bruker), XContentType.JSON);

        try {
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final AktorId aktoerId = AktorId.of(bruker.getAktoer_id());
        final Optional<GetResponse> getResponse = getDocument(aktoerId);

        assertThat(getResponse).isPresent();
        assertThat(getResponse.get()).isNotNull();
    }

    public Optional<GetResponse> getDocument(AktorId aktoerId) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(indexName.getValue());
        getRequest.id(aktoerId.toString());
        try {
            return Optional.of(restHighLevelClient.get(getRequest, RequestOptions.DEFAULT));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static void pollOpensearchUntil(Supplier<Boolean> func) {
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
        updateRequest.id(aktoerId.toString());
        updateRequest.doc(content);

        restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
    }
}
