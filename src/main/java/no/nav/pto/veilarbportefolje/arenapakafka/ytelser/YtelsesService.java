package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.*;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaAktivitetUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class YtelsesService {
    @NonNull @Qualifier("systemClient")
    private final AktorClient aktorClient;
    private final BrukerDataService brukerDataService;
    private final ElasticIndexer elasticIndexer;

    public void behandleKafkaDagPengerRecord(ConsumerRecord<String, YtelsesDTO> kafkaMelding) {
        YtelsesDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );

        //TODO: STIAN fix flag <3
        behandleKafkaMelding(melding, "DAGPENGER");
    }

    public void behandleKafkaMelding(YtelsesDTO kafkaMelding) {
        YtelsesInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());

        brukerDataService.oppdaterYtelser(aktorId, innhold, skalSlettesGoldenGate(kafkaMelding));
        elasticIndexer.indekser(aktorId);
    }

    public void behandleKafkaMelding(YtelseAAPDTO kafkaMelding) {

    }


    private boolean erGammelMelding(YtelsesDTO kafkaMelding, YtelsesInnhold innhold) {
        return false;
    }
}
