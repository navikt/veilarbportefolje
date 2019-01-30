package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Gauge;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

@Component
@Slf4j
public class MetricsReporter {

    private UnleashService unleash;

    private RestHighLevelClient elastic;

    @Inject
    public MetricsReporter(UnleashService unleash, RestHighLevelClient elastic) {
        this.unleash = unleash;
        this.elastic = elastic;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        scheduler.scheduleAtFixedRate(this::getNumberOfDocs, 1, 1, MINUTES);

        Gauge.builder("veilarbelastic_number_of_docs", this::getNumberOfDocs)
                .register(MetricsFactory.getMeterRegistry());
    }

    @SneakyThrows
    private long getNumberOfDocs() {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(0);
        searchRequest.indices(IndekseringConfig.getAlias());
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = elastic.search(searchRequest, RequestOptions.DEFAULT);
        return response.getHits().totalHits;
    }
}
