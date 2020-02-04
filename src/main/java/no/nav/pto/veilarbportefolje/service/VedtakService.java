package no.nav.pto.veilarbportefolje.service;

import no.nav.pto.veilarbportefolje.database.VedtakStatusRepository;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;

import javax.inject.Inject;

public class VedtakService {

    private VedtakStatusRepository vedtakStatusRepository;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository) {
        this.vedtakStatusRepository = vedtakStatusRepository;
    }

    public void behandleMelding(KafkaVedtakStatusEndring melding) {
        KafkaVedtakStatusEndring.KafkaVedtakStatus vedtakStatus = melding.getVedtakStatus();
        switch (vedtakStatus) {
            case UTKAST_SLETTET:
                vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
                break;
            case UTKAST_OPPRETTET:
                vedtakStatusRepository.insertVedtak(melding);
                break;
            case SENDT_TIL_BRUKER:
                vedtakStatusRepository.slettGamleVedtakOgUtkast(melding);
                vedtakStatusRepository.insertVedtak(melding);
                break;
            case SENDT_TIL_BESLUTTER:
                vedtakStatusRepository.updateVedtak(melding);
                break;
        }
    }
}
