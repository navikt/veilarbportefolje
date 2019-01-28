package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.springframework.stereotype.Component;

import static org.elasticsearch.cluster.health.ClusterHealthStatus.GREEN;

@Component
public class ElasticSearchHelsesjekk implements Helsesjekk {

    @Override
    public void helsesjekk() {
        ClusterHealthResponse health = ElasticUtils.clusterHealthcheck();
        if (health.getStatus() != GREEN) {
            throw new RuntimeException(health.toString());
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "elasticsearch helsesjekk",
                String.format("https://%s/%s", IndekseringConfig.getElasticHostname(), IndekseringConfig.getAlias()),
                "Sjekker helsestatus til Elasticsearch-clusteret",
                true
        );
    }
}
