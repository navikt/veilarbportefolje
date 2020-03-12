package no.nav.pto.veilarbportefolje.elastic;

import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

public class OpenDistroClient extends RestHighLevelClient {
    public OpenDistroClient(RestClientBuilder restClientBuilder) {
        super(restClientBuilder);
    }
}
