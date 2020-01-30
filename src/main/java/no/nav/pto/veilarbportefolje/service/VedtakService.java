package no.nav.pto.veilarbportefolje.service;

import no.nav.pto.veilarbportefolje.database.VedtakStatusRepository;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;

import javax.inject.Inject;

public class VedtakService {

    @Inject
    private VedtakStatusRepository vedtakStatusRepository;

    public void behandleMelding(KafkaVedtakStatusEndring melding) {
        if (melding.getVedtakStatus().equals(KafkaVedtakStatusEndring.KafkaVedtakStatus.UTKAST_SLETTET)) {
            vedtakStatusRepository.slettVedtakUtkast(melding);
        } else if (melding.getVedtakStatus().equals(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BESLUTTER)) {
            vedtakStatusRepository.slettVedtak(melding);
            vedtakStatusRepository.upsertVedtak(melding);
        } else {
            vedtakStatusRepository.upsertVedtak(melding);
        }
    }
}
