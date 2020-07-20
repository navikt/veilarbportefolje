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
    private final UnleashService unleashService;

    @Autowired
    public KafkaAktivitetService (AktivitetService aktivitetService, UnleashService unleashService) {
        this.aktivitetService = aktivitetService;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if(!unleashService.isEnabled("portefolje.behandle.aktivitet.kafkamelding")) {
            return;
        }
        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);

        if(skallIkkeOppdatereAktivitet(aktivitetData)) {
            return;
        }

        aktivitetService.oppdaterAktiviteter(aktivitetData);
    }

    private boolean skallIkkeOppdatereAktivitet(KafkaAktivitetMelding aktivitetData) {
        return !aktivitetData.isAvtalt() || erEnNyOpprettetAktivitet(aktivitetData);
    }

    private boolean erEnNyOpprettetAktivitet(KafkaAktivitetMelding aktivitetData) {
        return aktivitetData.getEndretDato() == null;
    }
}
