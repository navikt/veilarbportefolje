package no.nav.pto.veilarbportefolje.aktviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;

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

        if(skallIkkeOppdatereAktivitet(aktivitetData)) {
            return;
        }

        aktivitetService.oppdaterAktiviteter(Collections.singletonList(mapTilAktivitetDataFraFeed(aktivitetData)));
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }


    public static AktivitetDataFraFeed mapTilAktivitetDataFraFeed (KafkaAktivitetMelding kafkaAktivitetMelding) {
        Timestamp endretDato = Optional.ofNullable(kafkaAktivitetMelding.getEndretDato())
                .map(dato -> Timestamp.from(dato.toInstant()))
                .orElse(Timestamp.valueOf(LocalDateTime.now()));

        return new AktivitetDataFraFeed()
                .setAktivitetId(kafkaAktivitetMelding.getAktivitetId())
                .setAktorId(kafkaAktivitetMelding.getAktorId())
                .setFraDato(dateToTimestamp(kafkaAktivitetMelding.getFraDato()))
                .setTilDato(dateToTimestamp(kafkaAktivitetMelding.getTilDato()))
                .setEndretDato(endretDato)
                .setAktivitetType(kafkaAktivitetMelding.getAktivitetType().name())
                .setStatus(kafkaAktivitetMelding.getAktivitetStatus().name())
                .setHistorisk(kafkaAktivitetMelding.isHistorisk())
                .setAvtalt(kafkaAktivitetMelding.isAvtalt());
    }

    private boolean skallIkkeOppdatereAktivitet(KafkaAktivitetMelding aktivitetData) {
        return !aktivitetData.isAvtalt() || erEnNyOpprettetAktivitet(aktivitetData);
    }

    private boolean erEnNyOpprettetAktivitet(KafkaAktivitetMelding aktivitetData) {
        return aktivitetData.getEndretDato() == null;
    }
}
