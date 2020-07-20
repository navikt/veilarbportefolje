package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Service
public class ElasticServiceV2 {

    private final String alias;
    private RestHighLevelClient restHighLevelClient;

    @Inject
    public ElasticServiceV2(RestHighLevelClient restHighLevelClient, String alias) {
        this.client = restHighLevelClient;
        this.alias = alias;
    }

    @SneakyThrows
    public void updateHarDeltCv(Fnr fnr, boolean harDeltCv) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(alias);
        updateRequest.type("_doc");
        updateRequest.id(fnr.getFnr());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("har_delt_cv", harDeltCv)
                .endObject()
        );

        try {
            restHighLevelClient.update(updateRequest, DEFAULT);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument ved oppdatering av cv");
            }
        }
    }
}
