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
    ObjectMapper objectMapper = JsonProvider.createObjectMapper();

    public KafkaAktivitetService (AktivitetService aktivitetService, UnleashService unleashService) {
        this.aktivitetService = aktivitetService;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        /*
        if(!unleashService.isEnabled("portefolje.behandle.kafkamelding")) {
            return;
        }

         */

        try {
            long startTime = System.currentTimeMillis();
            log.info("Inne i aktivitetmelding");
            List<AktivitetDataFraFeed> aktivitetData = objectMapper.readValue(kafkaMelding, objectMapper.getTypeFactory().constructParametricType(List.class, AktivitetDataFraFeed.class));
            aktivitetService.oppdaterAktiviteter(aktivitetData);
            long endTime = System.currentTimeMillis();
            log.info("Oppdater aktiviteter tog {} millisekunder att exekvera", (endTime-startTime));
        } catch (JsonProcessingException e) {
            log.error("Kunde ikke deserialisera kafka aktivitetmelding: {}", kafkaMelding);
        }
    }
}
