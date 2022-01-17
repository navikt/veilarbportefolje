package no.nav.pto.veilarbportefolje.opensearch;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
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

import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer.BATCH_SIZE;
import static org.opensearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;

@Slf4j
@Service
public class OpensearchAdminService {
    private final RestHighLevelClient restHighLevelClient;
    private final OpensearchIndexer opensearchIndexer;
    private final OppfolgingRepository oppfolgingRepository;

    @Autowired
    public OpensearchAdminService(RestHighLevelClient restHighLevelClient, OpensearchIndexer opensearchIndexer, OppfolgingRepository oppfolgingRepository) {
        this.restHighLevelClient = restHighLevelClient;
        this.opensearchIndexer = opensearchIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
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
    private String readJsonFromFileStream(InputStream settings) {
        return IOUtils.toString(settings, String.valueOf(StandardCharsets.UTF_8));
    }

    public void testSkrivMedNyeSettings() {
        final int ANTALL_BRUKERE = 100_000;
        String testIndex = opprettNyIndeks("slett_meg_" + createIndexName());

        log.info("Hovedindekserings (test): bruker index: {}", testIndex);
        List<AktorId> brukere = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        brukere = brukere.subList(0, Math.min(ANTALL_BRUKERE, brukere.size()));

        long tidsStempel0 = System.currentTimeMillis();
        boolean oppdatertSettings = oppdaterRefreshInterval(testIndex, true);
        if (oppdatertSettings) {
            log.info("Hovedindeksering (test): Indekserer {} brukere", brukere.size());
            partition(brukere, BATCH_SIZE).forEach(opensearchIndexer::indekserBolk);

            boolean resetSettings = oppdaterRefreshInterval(testIndex, false);
            log.info("Hovedindeksering (test): resetSetting: {} ", resetSettings);
        } else {
            log.info("Hovedindeksering (test): fikk ikke oppdatertsettings");
        }

        long tidsStempel1 = System.currentTimeMillis();
        long tid = tidsStempel1 - tidsStempel0;
        log.info("Hovedindekserings (test): Ferdig på {} ms, indekserte {} brukere", tid, brukere.size());
    }
}
