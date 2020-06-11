package no.nav.pto.veilarbportefolje.aktviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import java.sql.Timestamp;
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
        if(!unleashService.isEnabled("portefolje.behandle.aktivitet.kafkamelding")) {
            return;
        }
        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);
        aktivitetService.oppdaterAktiviteter(Collections.singletonList(mapTilAktivitetDataFraFeed(aktivitetData)));
    }


    private AktivitetDataFraFeed mapTilAktivitetDataFraFeed (KafkaAktivitetMelding kafkaAktivitetMelding) {
        return new AktivitetDataFraFeed()
                .setAktivitetId(kafkaAktivitetMelding.getAktivitetId())
                .setAktorId(kafkaAktivitetMelding.getAktorId())
                .setFraDato(Timestamp.from(kafkaAktivitetMelding.getFraDato().toInstant()))
                .setTilDato(Timestamp.from(kafkaAktivitetMelding.getTilDato().toInstant()))
                .setEndretDato(Timestamp.from(kafkaAktivitetMelding.getEndretDato().toInstant()))
                .setAktivitetType(kafkaAktivitetMelding.getAktivitetType().name())
                .setStatus(kafkaAktivitetMelding.getAktivitetStatus().name())
                .setHistorisk(kafkaAktivitetMelding.isHistorisk())
                .setAvtalt(kafkaAktivitetMelding.isAvtalt());
    }
}
