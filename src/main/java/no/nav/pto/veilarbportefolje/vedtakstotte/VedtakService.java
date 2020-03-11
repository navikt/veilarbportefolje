package no.nav.pto.veilarbportefolje.vedtakstotte;

import io.vavr.control.Option;
import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;

import java.io.IOException;

public class VedtakService {

    private VedtakStatusRepository vedtakStatusRepository;
    private ElasticIndexer elasticIndexer;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
    }

    public void behandleMelding(KafkaVedtakStatusEndring melding) {
        KafkaVedtakStatusEndring.KafkaVedtakStatus vedtakStatus = melding.getVedtakStatus();
        switch (vedtakStatus) {
            case UTKAST_SLETTET:

                break;
            case UTKAST_OPPRETTET:
            case SENDT_TIL_BESLUTTER:
                vedtakStatusRepository.upsertVedtak(melding);
                break;
            case SENDT_TIL_BRUKER:
                vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
                vedtakStatusRepository.upsertVedtak(melding);
                break;
        }
        elasticIndexer.indekser(AktoerId.of(melding.getAktorId()));
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        elasticIndexer.oppdaterBrukerFeldt(new UpdateByQueryRequest())
    }
}
