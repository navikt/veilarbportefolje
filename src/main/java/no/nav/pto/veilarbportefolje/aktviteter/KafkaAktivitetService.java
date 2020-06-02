package no.nav.pto.veilarbportefolje.aktviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import java.util.Collections;

import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class KafkaAktivitetService implements KafkaConsumerService<String> {
    AktivitetService aktivitetService;
    UnleashService unleashService;

    public KafkaAktivitetService (AktivitetService aktivitetService, UnleashService unleashService) {
        this.aktivitetService = aktivitetService;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if(!unleashService.isEnabled("portefolje.behandle.kafkamelding")) {
            return;
        }
        AktivitetDataFraFeed aktivitetData = fromJson(kafkaMelding, AktivitetDataFraFeed.class);
        aktivitetService.oppdaterAktiviteter(Collections.singletonList(aktivitetData));
    }
}
