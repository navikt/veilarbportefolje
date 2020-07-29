package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;

@Slf4j
@Service
public class KafkaAktivitetService implements KafkaConsumerService<String> {
    private final AktivitetService aktivitetService;

    @Autowired
    public KafkaAktivitetService (AktivitetService aktivitetService) {
        this.aktivitetService = aktivitetService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {

        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);
        log.info("aktivitetmelding {}", kafkaMelding);

        if(skallIkkeOppdatereAktivitet(aktivitetData)) {
            return;
        }

        aktivitetService.oppdaterAktiviteter(aktivitetData);
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }

    private boolean skallIkkeOppdatereAktivitet(KafkaAktivitetMelding aktivitetData) {
        return !aktivitetData.isAvtalt() || erEnNyOpprettetAktivitet(aktivitetData);
    }

    private boolean erEnNyOpprettetAktivitet(KafkaAktivitetMelding aktivitetData) {
        return aktivitetData.getEndretDato() == null;
    }
}
