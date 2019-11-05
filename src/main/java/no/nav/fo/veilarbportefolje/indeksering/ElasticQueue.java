package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.indeksering.domene.OppfolgingsBruker;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static no.nav.fo.veilarbportefolje.indeksering.ElasticConfig.defaultConfig;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.getAlias;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Slf4j
public class ElasticQueue {

    private static LinkedBlockingQueue<OppfolgingsBruker> queue = new LinkedBlockingQueue<>();
    private static List<OppfolgingsBruker> buffer = new ArrayList<>();
    private static RestHighLevelClient client = ElasticConfig.createClient(defaultConfig);

    private static Counter writeCounter = Counter.builder("portefolje_elastic_write_operations").register(getMeterRegistry());

    static {

        Gauge.builder("portefolje_elastic_queue_size", () -> queue.size()).register(getMeterRegistry());

        new Thread(() -> {
            queue.drainTo(buffer);

            List<String> aktoerIder = buffer.stream()
                    .map(OppfolgingsBruker::getAktoer_id)
                    .collect(Collectors.toList());

            log.info("Henter ut brukere med aktøerId-er {} fra elastic-kø", aktoerIder);

            flushBufferToElastic();
        });

    }

    public void add(OppfolgingsBruker bruker) {
        log.info("Putter bruker med aktørid {} i elastic-kø", bruker.getAktoer_id());
        queue.add(bruker);
    }

    public void add(List<OppfolgingsBruker> bruker) {

        List<String> aktoerIder = bruker.stream()
                .map(OppfolgingsBruker::getAktoer_id)
                .collect(Collectors.toList());

        log.info("Putter brukere med aktørid-er {} i elastic-kø", aktoerIder);
        queue.addAll(bruker);
    }

    @SneakyThrows
    private static void flushBufferToElastic() {
        BulkRequest bulk = new BulkRequest();
        buffer.stream()
                .map(bruker -> new IndexRequest(
                        getAlias(),
                        "_doc",
                        bruker.getFnr()).
                        source(toJson(bruker), XContentType.JSON)
                )
                .forEach(bulk::add);

        BulkResponse response = client.bulk(bulk, DEFAULT);

        writeCounter.increment();

        if (response.hasFailures()) {
            throw new RuntimeException(response.buildFailureMessage());
        }

        buffer.clear();

        int WRITE_DELAY_IN_MILLISECONDS = 10_000;
        Thread.sleep(WRITE_DELAY_IN_MILLISECONDS);
    }

}
