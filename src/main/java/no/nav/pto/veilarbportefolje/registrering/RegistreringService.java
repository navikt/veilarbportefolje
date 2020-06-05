package no.nav.pto.veilarbportefolje.registrering;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class RegistreringService implements KafkaConsumerService<ArbeidssokerRegistrertEvent> {
    private final RegistreringRepository registreringRepository;
    private final RestHighLevelClient client;
    private final AktoerService aktoerService;

    public RegistreringService(RegistreringRepository registreringRepository, RestHighLevelClient client, AktoerService aktoerService) {
        this.registreringRepository = registreringRepository;
        this.client = client;
        this.aktoerService = aktoerService;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        registreringRepository.upsertBrukerRegistrering(kafkaRegistreringMelding);
        aktoerService.hentFnrFraAktorId(AktoerId.of(kafkaRegistreringMelding.getAktorid()))
                .onSuccess(fnr -> oppdaterElasticMedRegistreringData(fnr, kafkaRegistreringMelding));
    }

    private XContentBuilder mapTilRegistreringJson(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .field("brukers_situasjon", kafkaRegistreringMelding.getBrukersSituasjon())
                        .field("aktoer_id", kafkaRegistreringMelding.getAktorid())
                        .endObject())
                .get();
    }

    private void oppdaterElasticMedRegistreringData(Fnr fnr, ArbeidssokerRegistrertEvent arbeidssokerRegistrertEvent) {
        XContentBuilder registreringJson = mapTilRegistreringJson(arbeidssokerRegistrertEvent);
        UpdateRequest updateRequest = new UpdateRequest()
                .index(getAlias())
                .type("_doc")
                .id(fnr.getFnr())
                .retryOnConflict(1)
                .doc(registreringJson);

        Try.of(()-> client.update(updateRequest, DEFAULT))
                .onFailure(err -> log.error("Feil vid skrivning til indeks vid registreing melding", err));
    }

    public Optional<ZonedDateTime> hentRegistreringOpprettet(AktoerId aktoerId) {
        return registreringRepository
                .hentBrukerRegistrering(aktoerId)
                .map(ArbeidssokerRegistrertEvent::getRegistreringOpprettet)
                .map(ZonedDateTime::parse);
    }
}
