package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Service
public class VedtakService implements KafkaConsumerService<String> {

    private final VedtakStatusRepository vedtakStatusRepository;
    private final ElasticIndexer elasticIndexer;
    private final AtomicBoolean rewind;

    @Autowired
    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
        this.rewind = new AtomicBoolean();
    }

    public void behandleKafkaMelding(String melding) {
        KafkaVedtakStatusEndring vedtakStatusEndring = fromJson(melding, KafkaVedtakStatusEndring.class);
        KafkaVedtakStatusEndring.VedtakStatusEndring vedtakStatus = vedtakStatusEndring.getVedtakStatusEndring();
        switch (vedtakStatus) {
            case UTKAST_SLETTET : {
                slettUtkast(vedtakStatusEndring);
                break;
            }
            case VEDTAK_SENDT: {
                setVedtakSendt(vedtakStatusEndring);
                break;
            }
            case UTKAST_OPPRETTET:
            case OVERTA_FOR_VEILEDER:{
                oppdaterUtkastMedAnsvarligVeileder(vedtakStatusEndring);
                break;
            }
            case BESLUTTER_PROSESS_STARTET:
            case BLI_BESLUTTER:
            case GODKJENT_AV_BESLUTTER:
            case KLAR_TIL_BESLUTTER:
            case KLAR_TIL_VEILEDER:
            {
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
    }

    private void oppdaterUtkastMedAnsvarligVeileder(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtakMedAnsvarligVeileder(melding);
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
    }

    private void setVedtakSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
    }

}
