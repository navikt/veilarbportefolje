package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Component;

import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.cluster.health.ClusterHealthStatus.GREEN;

@Component
public class ElasticSearchHelsesjekk implements Helsesjekk {

    private RestHighLevelClient elastic;

    public ElasticSearchHelsesjekk(RestHighLevelClient elastic) {
        this.elastic = elastic;
    }

    @Override
    public void helsesjekk() throws Throwable {
        ClusterHealthResponse health = elastic.cluster().health(new ClusterHealthRequest(), DEFAULT);
        if (health.getStatus() != GREEN) {
            throw new RuntimeException(health.toString());
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "elasticsearch helsesjekk",
                String.format("https://%s/%s", IndekseringConfig.getElasticHostname(), IndekseringConfig.ALIAS),
                "Sjekker helsestatus til Elasticsearch-clusteret",
                true
        );
    }
}
