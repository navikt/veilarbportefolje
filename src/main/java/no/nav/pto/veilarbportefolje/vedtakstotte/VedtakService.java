package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
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
                vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
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
}
