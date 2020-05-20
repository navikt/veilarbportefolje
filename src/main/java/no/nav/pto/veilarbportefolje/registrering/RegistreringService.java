package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.util.DateUtils;

public class RegistreringService implements KafkaConsumerService<ArbeidssokerRegistrertEvent> {
    private RegistreringRepository registreringRepository;
    private ElasticIndexer elasticIndexer;

    public RegistreringService(RegistreringRepository registreringRepository, ElasticIndexer elasticIndexer) {
        this.registreringRepository = registreringRepository;
        this.elasticIndexer = elasticIndexer;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        ArbeidssokerRegistrertEvent brukerRegistrering = registreringRepository.hentBrukerRegistrering(AktoerId.of(kafkaRegistreringMelding.getAktorid()));
        AktoerId aktoerId = AktoerId.of(kafkaRegistreringMelding.getAktorid());

        if(harRegistreringsDato(brukerRegistrering)) {
            if (erNyereRegistering(brukerRegistrering, kafkaRegistreringMelding)) {
                registreringRepository.oppdaterBrukerRegistring(kafkaRegistreringMelding);
                elasticIndexer.indekserAsynkront(aktoerId);
            }
            return;
        }
        registreringRepository.insertBrukerRegistrering(kafkaRegistreringMelding);
        elasticIndexer.indekserAsynkront(aktoerId);

    }

    private boolean erNyereRegistering(ArbeidssokerRegistrertEvent gjeldendeBrukerRegistrering, ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        return DateUtils.dateIfAfterAnotherDate(kafkaRegistreringMelding.getRegistreringOpprettet(), gjeldendeBrukerRegistrering.getRegistreringOpprettet());
    }

    private boolean harRegistreringsDato (ArbeidssokerRegistrertEvent brukerRegistrering) {
        return brukerRegistrering != null && brukerRegistrering.getRegistreringOpprettet() != null;
    }

}
