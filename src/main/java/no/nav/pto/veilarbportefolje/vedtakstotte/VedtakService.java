package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.AktoerService;

public class VedtakService {

    private VedtakStatusRepository vedtakStatusRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
    }

    public void behandleMelding(KafkaVedtakStatusEndring melding) {
        KafkaVedtakStatusEndring.KafkaVedtakStatus vedtakStatus = melding.getVedtakStatus();
        switch (vedtakStatus) {
            case UTKAST_SLETTET : {
                slettUtkast(melding);
                return;
            }
            case UTKAST_OPPRETTET:
            case SENDT_TIL_BESLUTTER: {
                oppdaterUtkast(melding);
                return;
            }
            case SENDT_TIL_BRUKER: {
                setUtkastTilSendt(melding);
            }
        }
    }

    @SneakyThrows
    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        OppfolgingsBruker oppfolgingsBruker = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId()))
                .map(fnr -> new OppfolgingsBruker()
                        .setVedtak_status(null)
                        .setVedtak_status_endret(null)
                        .setFnr(fnr.toString()))
                .get();
        elasticIndexer.oppdaterBrukerDoc(oppfolgingsBruker);
    }

    @SneakyThrows
    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        OppfolgingsBruker oppfolgingsBruker = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId()))
                .map(fnr -> new OppfolgingsBruker()
                        .setVedtak_status(melding.getVedtakStatus().name())
                        .setVedtak_status_endret(melding.getStatusEndretTidspunkt().toString())
                        .setFnr(fnr.toString()))
                .get();
        elasticIndexer.oppdaterBrukerDoc(oppfolgingsBruker);
    }

    @SneakyThrows
    private void setUtkastTilSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        OppfolgingsBruker oppfolgingsBruker = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId()))
                .map(fnr -> new OppfolgingsBruker()
                        .setVedtak_status(null)
                        .setVedtak_status_endret(null)
                        .setFnr(fnr.toString()))
                .get();
        elasticIndexer.oppdaterBrukerDoc(oppfolgingsBruker);
    }

}
