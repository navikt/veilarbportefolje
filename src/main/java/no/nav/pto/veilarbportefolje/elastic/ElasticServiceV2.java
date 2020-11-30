package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
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

    private final String indeks;
    private final RestHighLevelClient restHighLevelClient;

    @Autowired
    public ElasticServiceV2(RestHighLevelClient restHighLevelClient, ElasticIndex elasticIndex) {
        this.restHighLevelClient = restHighLevelClient;
        this.indeks = elasticIndex.getIndex();
    }

    @SneakyThrows
    public void updateHarDeltCv(Fnr fnr, boolean harDeltCv) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indeks);
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

    @SneakyThrows
    public void updateRegistering(Fnr fnr, ArbeidssokerRegistrertEvent utdanningEvent) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indeks);
        updateRequest.id(fnr.getFnr());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("brukers_situasjon", utdanningEvent.getBrukersSituasjon())
                .field("utdanning", utdanningEvent.getUtdanning())
                .field("utdanning_bestatt", utdanningEvent.getUtdanningBestatt())
                .field("utdanning_godkjent", utdanningEvent.getUtdanningGodkjent())
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
