package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class ElasticServiceV2 {

    private final IndexName indexName;
    private final RestHighLevelClient restHighLevelClient;

    public ElasticServiceV2(RestHighLevelClient restHighLevelClient, IndexName indexName) {
        this.restHighLevelClient = restHighLevelClient;
        this.indexName = indexName;
    }

    public IndexName getIndexName() {
        return indexName;
    }

    @SneakyThrows
    public void updateRegistering(AktoerId aktoerId, ArbeidssokerRegistrertEvent utdanningEvent) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("brukers_situasjon", utdanningEvent.getBrukersSituasjon())
                .field("utdanning", utdanningEvent.getUtdanning())
                .field("utdanning_bestatt", utdanningEvent.getUtdanningBestatt())
                .field("utdanning_godkjent", utdanningEvent.getUtdanningGodkjent())
                .endObject();

        update(aktoerId, content);
    }

    @SneakyThrows
    public void updateHarDeltCv(AktoerId aktoerId, boolean harDeltCv) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("har_delt_cv", harDeltCv)
                .endObject();

        update(aktoerId, content);
    }

    @SneakyThrows
    public void markerBrukerSomSlettet(AktoerId aktoerId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("oppfolging", false)
                .endObject();

        update(aktoerId, content);
    }

    @SneakyThrows
    public void settManuellStatus(AktoerId aktoerId, ManuellBrukerStatus manuellStatus) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("manuell_bruker", manuellStatus.name())
                .endObject();

        update(aktoerId, content);
    }

    @SneakyThrows
    public void oppdaterNyForVeileder(AktoerId aktoerId, boolean nyForVeileder) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("ny_for_veileder", nyForVeileder)
                .endObject();

        update(aktoerId, content);
    }

    @SneakyThrows
    public void oppdaterVeileder(AktoerId aktoerId, VeilederId veilederId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("veileder_id", veilederId.toString())
                .field("ny_for_enhet", false)
                .endObject();

        update(aktoerId, content);
    }

    @SneakyThrows
    public void slettArbeidsliste(AktoerId aktoerId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv", false)
                .field("arbeidsliste_sist_endret_av_veilederid", (String)null)
                .field("arbeidsliste_endringstidspunkt", (String)null)
                .field("arbeidsliste_kommentar", (String)null)
                .field("arbeidsliste_overskrift", (String)null)
                .field("arbeidsliste_frist", (String)null)
                .field("arbeidsliste_kategori", (String)null)
                .endObject();

        update(aktoerId, content);
    }

    private void update(AktoerId aktoerId, XContentBuilder content) throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indexName.getValue());
        updateRequest.type("_doc");
        updateRequest.id(aktoerId.toString());
        updateRequest.doc(content);

        try {
            final UpdateResponse update = restHighLevelClient.update(updateRequest, DEFAULT);
            log.info("Oppdaterte dokument for bruker {} returnerte status {}", aktoerId, update.status());
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.warn("Kunne ikke finne dokument for bruker {} ved oppdatering av indeks", aktoerId.toString());
            }
        }
    }

}
