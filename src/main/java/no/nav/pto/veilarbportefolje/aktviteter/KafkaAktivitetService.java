package no.nav.pto.veilarbportefolje.aktviteter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.json.JsonProvider;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import java.util.List;

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
        ObjectMapper objectMapper = JsonProvider.createObjectMapper();
        try {
            List<AktivitetDataFraFeed> aktivitetData = objectMapper.readValue(kafkaMelding, objectMapper.getTypeFactory().constructParametricType(List.class, AktivitetDataFraFeed.class));
            aktivitetService.oppdaterAktiviteter(aktivitetData);
        } catch (JsonProcessingException e) {
            log.error("Kunne ikke desirialisera kafka aktiviitetmelding: {}", kafkaMelding);
        }
    }
}
