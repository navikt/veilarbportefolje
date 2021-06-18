package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Slf4j
@Service
@RequiredArgsConstructor
public class VedtakService implements KafkaConsumerService<String> {

    private final VedtakStatusRepository vedtakStatusRepository;
    private final VedtakStatusRepositoryV2 vedtakStatusRepositoryV2;
    private final ElasticIndexer elasticIndexer;
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean();

    public void behandleKafkaMelding(String melding) {
        KafkaVedtakStatusEndring vedtakStatusEndring = fromJson(melding, KafkaVedtakStatusEndring.class);
        KafkaVedtakStatusEndring.VedtakStatusEndring vedtakStatus = vedtakStatusEndring.getVedtakStatusEndring();
        switch (vedtakStatus) {
            case UTKAST_SLETTET: {
                slettUtkast(vedtakStatusEndring);
                break;
            }
            case VEDTAK_SENDT: {
                setVedtakSendt(vedtakStatusEndring);
                break;
            }
            case UTKAST_OPPRETTET:
                opprettUtkast(vedtakStatusEndring);
                break;
            case OVERTA_FOR_VEILEDER:
                oppdaterAnsvarligVeileder(vedtakStatusEndring);
                break;
            case BESLUTTER_PROSESS_STARTET:
            case BESLUTTER_PROSESS_AVBRUTT:
            case BLI_BESLUTTER:
            case GODKJENT_AV_BESLUTTER:
            case KLAR_TIL_BESLUTTER:
            case KLAR_TIL_VEILEDER: {
                oppdaterUtkast(vedtakStatusEndring);
                break;
            }
        }
        elasticIndexer.indekser(AktorId.of(vedtakStatusEndring.getAktorId()));
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);

    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());

        if (erPostgresPa(unleashService))
            vedtakStatusRepositoryV2.slettGamleVedtakOgUtkast(melding.getAktorId());
    }

    private void opprettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.opprettUtkast(melding);

        if (erPostgresPa(unleashService))
            vedtakStatusRepositoryV2.upsertVedtak(melding);
    }

    private void oppdaterAnsvarligVeileder(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.oppdaterAnsvarligVeileder(melding);

        if (erPostgresPa(unleashService))
            vedtakStatusRepositoryV2.oppdaterAnsvarligVeileder(melding);
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);

        if (erPostgresPa(unleashService))
            vedtakStatusRepositoryV2.updateVedtak(melding);
    }

    private void setVedtakSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);

        if (erPostgresPa(unleashService)) {
            vedtakStatusRepositoryV2.slettGamleVedtakOgUtkast(melding.getAktorId());
        }
    }

}
