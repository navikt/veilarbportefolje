package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static no.nav.common.rest.client.RestClient.baseClient;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchCountService.createAbsoluteUrl;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchCountService.getAuthHeaderValue;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer.BATCH_SIZE;
import static org.opensearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;

@Slf4j
@Service
public class OpensearchAdminService {
    private final RestHighLevelClient restHighLevelClient;
    private final OpensearchIndexer opensearchIndexer;
    private final OppfolgingRepository oppfolgingRepository;
    private final OpensearchClientConfig openSearchClientConfig;
    private final OkHttpClient httpClient;

    @Autowired
    public OpensearchAdminService(RestHighLevelClient restHighLevelClient, OpensearchIndexer opensearchIndexer, OppfolgingRepository oppfolgingRepository, OpensearchClientConfig openSearchClientConfig) {
        this.restHighLevelClient = restHighLevelClient;
        this.opensearchIndexer = opensearchIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
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
    public void opprettAliasForIndeks(String indeks) {
        IndicesAliasesRequest.AliasActions addAliasAction = new IndicesAliasesRequest.AliasActions(ADD)
                .index(indeks)
                .alias(BRUKERINDEKS_ALIAS);

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
        AcknowledgedResponse response = restHighLevelClient.indices().updateAliases(request, RequestOptions.DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke legge til alias {}", BRUKERINDEKS_ALIAS);
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    public boolean oppdaterRefreshInterval(String indexName, boolean optimalBatch) {
        String value = optimalBatch ? "-1" : "10s";
        UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest(indexName)
                .settings(
                        Settings.builder()
                                .put("refresh_interval", value)
                                .build()
                );
        return restHighLevelClient.indices().putSettings(updateSettingsRequest, RequestOptions.DEFAULT).isAcknowledged();
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

    @SneakyThrows
    public String getSettingsOnIndex(String indexName) {
        String url = createAbsoluteUrl(openSearchClientConfig, indexName) + "_settings?include_defaults=true";
        Request request = new Request.Builder()
                .url(url).get()
                .addHeader("Authorization", getAuthHeaderValue(openSearchClientConfig))
                .build();

        return callAndGetBody(request);
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

    public void testSkrivMedNyeSettings() {
        // Test: Med batch satt til: 100
        String testIndex100 = opprettNyIndeks("slett_meg_100batch_" + createIndexName());
        skriv100_000BrukeretilIndex(new IndexName(testIndex100), 100);
        slettIndex(testIndex100);

        // Test: Med batch satt til: 200
        String testIndex200 = opprettNyIndeks("slett_meg_200batch_" + createIndexName());
        skriv100_000BrukeretilIndex(new IndexName(testIndex200), 200);
        slettIndex(testIndex200);

        // Test: Med batch satt til: 500
        String testIndex2 = opprettNyIndeks("slett_meg_500batch_" + createIndexName());
        skriv100_000BrukeretilIndex(new IndexName(testIndex2), 500);
        slettIndex(testIndex2);

        // Test: Med batch satt til: 1000
        String testIndex5 = opprettNyIndeks("slett_meg_standard_" + createIndexName());
        skriv100_000BrukeretilIndex(new IndexName(testIndex5), BATCH_SIZE);
        slettIndex(testIndex5);

        // Test: Med batch satt til: 2000
        String testIndex3 = opprettNyIndeks("slett_meg_2000batch_" + createIndexName());
        skriv100_000BrukeretilIndex(new IndexName(testIndex3), 2000);
        slettIndex(testIndex3);

        // Test: Med batch satt til: 4000
        String testIndex4 = opprettNyIndeks("slett_meg_5000batch_" + createIndexName());
        skriv100_000BrukeretilIndex(new IndexName(testIndex4), 4000);
        slettIndex(testIndex4);
    }

    private void skriv100_000BrukeretilIndex(IndexName testIndex, int batch_size) {
        long tidsStempel0 = System.currentTimeMillis();

        List<AktorId> brukere = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        brukere = brukere.subList(0, Math.min(100_000, brukere.size()));

        log.info("Hovedindeksering (test): Indekserer {} brukere", brukere.size());
        opensearchIndexer.indekserBolk(brukere, testIndex, batch_size);

        long tidsStempel1 = System.currentTimeMillis();
        long tid = tidsStempel1 - tidsStempel0;
        log.info("Hovedindekserings ({}): Ferdig på {} ms, indekserte {} brukere", testIndex.getValue(), tid, brukere.size());
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
