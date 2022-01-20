package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static no.nav.common.rest.client.RestClient.baseClient;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchCountService.createAbsoluteUrl;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchCountService.getAuthHeaderValue;
import static org.opensearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;

@Slf4j
@Service
public class OpensearchAdminService {
    private final RestHighLevelClient restHighLevelClient;
    private final OpensearchClientConfig openSearchClientConfig;
    private final OkHttpClient httpClient;

    @Autowired
    public OpensearchAdminService(RestHighLevelClient restHighLevelClient, OpensearchClientConfig openSearchClientConfig) {
        this.restHighLevelClient = restHighLevelClient;
        this.openSearchClientConfig = openSearchClientConfig;

        this.httpClient = baseClient();
    }

    @SneakyThrows
    public String opprettNyIndeks() {
        return opprettNyIndeks(createIndexName());
    }

    @SneakyThrows
    public String opprettNyIndeks(String indeksNavn) {
        String json = Optional.ofNullable(getClass()
                        .getResourceAsStream("/opensearch_settings.json"))
                .map(this::readJsonFromFileStream)
                .orElseThrow();

        CreateIndexRequest request = new CreateIndexRequest(indeksNavn)
                .source(json, XContentType.JSON);
        CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke opprette ny indeks {}", indeksNavn);
            throw new RuntimeException();
        }
        return indeksNavn;
    }

    private static String createIndexName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s", BRUKERINDEKS_ALIAS, timestamp);
    }

    @SneakyThrows
    public boolean slettIndex(String indexName) {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
        return restHighLevelClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT).isAcknowledged();
    }

    @SneakyThrows
    public void opprettAliasForIndeks(String indeks, Boolean isWriteIndex) {
        IndicesAliasesRequest.AliasActions addAliasAction = new IndicesAliasesRequest.AliasActions(ADD)
                .index(indeks)
                .alias(BRUKERINDEKS_ALIAS)
                .writeIndex(isWriteIndex);

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
        AcknowledgedResponse response = restHighLevelClient.indices().updateAliases(request, RequestOptions.DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke legge til alias {}", BRUKERINDEKS_ALIAS);
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    public String hentAliaser() {
        String url = createAbsoluteUrl(openSearchClientConfig) + "_aliases";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue(openSearchClientConfig))
                .build();

        return callAndGetBody(request);
    }

    public String hentBrukerIndex() {
        String url = createAbsoluteUrl(openSearchClientConfig) + "_cat/indices/" + BRUKERINDEKS_ALIAS + "/?h=index";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue(openSearchClientConfig))
                .build();

        String[] indexerPaAlias = Objects.requireNonNull(callAndGetBody(request)).split("\\r?\\n");
        if (indexerPaAlias.length != 1) {
            throw new IllegalStateException("Feil antall indexer på alias: " + indexerPaAlias.length);
        }
        return indexerPaAlias[0];

    }

    @SneakyThrows
    public String getSettingsOnIndex(String indexName) {
        String url = createAbsoluteUrl(openSearchClientConfig, indexName) + "_settings?include_defaults=true";
        Request request = new Request.Builder()
                .url(url).get()
                .addHeader("Authorization", getAuthHeaderValue(openSearchClientConfig))
                .build();

        return callAndGetBody(request);
    }

    public String opprettSkriveIndeksPaAlias() {
        String nyIndex = opprettNyIndeks();
        opprettAliasForIndeks(nyIndex, true);

        return nyIndex;
    }

    public void settAliasSettingsTilDefault(String indexNavn) {
        opprettAliasForIndeks(indexNavn, null);
    }

    @SneakyThrows
    public String updateFromReadOnlyMode() {
        String url = createAbsoluteUrl(openSearchClientConfig);
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(MEDIA_TYPE_JSON, """
                        {
                          "index": {
                            "blocks": {
                              "read_only_allow_delete": "false"
                            }
                          }
                        }
                        """))
                .addHeader("Authorization", getAuthHeaderValue(openSearchClientConfig))
                .build();

        return callAndGetBody(request);
    }

    @SneakyThrows
    public String forceShardAssignment() {
        String url = createAbsoluteUrl(openSearchClientConfig) + "_cluster/reroute?retry_failed=true";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue(openSearchClientConfig))
                .post(RequestBody.create(MEDIA_TYPE_JSON, "{}"))
                .header("Content-Length", "0")
                .build();

        return callAndGetBody(request);
    }

    @SneakyThrows
    private String readJsonFromFileStream(InputStream settings) {
        return IOUtils.toString(settings, String.valueOf(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    private String callAndGetBody(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    return null;
                }
                return responseBody.string();
            }
        }
    }
}
