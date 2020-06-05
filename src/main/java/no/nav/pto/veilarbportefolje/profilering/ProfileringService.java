package no.nav.pto.veilarbportefolje.profilering;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;

import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


@Slf4j
public class ProfileringService implements KafkaConsumerService<ArbeidssokerProfilertEvent> {
    private final ProfileringRepository profileringRepository;
    private final RestHighLevelClient client;
    private final AktoerService aktoerService;

    public ProfileringService(ProfileringRepository profileringRepository, RestHighLevelClient client, AktoerService aktoerService) {
        this.profileringRepository = profileringRepository;
        this.client = client;
        this.aktoerService = aktoerService;
    }

    public void behandleKafkaMelding (ArbeidssokerProfilertEvent kafkaMelding) {
        profileringRepository.upsertBrukerProfilering(kafkaMelding);
        aktoerService.hentFnrFraAktorId(AktoerId.of(kafkaMelding.getAktorid()))
                .onSuccess(fnr -> oppdaterElasticMedProfileringData(fnr, kafkaMelding));
    }

    private XContentBuilder mapTilRegistreringJson(ArbeidssokerProfilertEvent kafkaMelding) {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .field("profilering_resultat", kafkaMelding.getProfilertTil().name())
                        .field("aktoer_id", kafkaMelding.getAktorid())
                        .endObject())
                .get();
    }

    private void oppdaterElasticMedProfileringData(Fnr fnr, ArbeidssokerProfilertEvent arbeidssokerProfilertEvent) {
        XContentBuilder registreringJson = mapTilRegistreringJson(arbeidssokerProfilertEvent);
        UpdateRequest updateRequest = new UpdateRequest()
                .index(getAlias())
                .type("_doc")
                .id(fnr.getFnr())
                .retryOnConflict(1)
                .doc(registreringJson);

        Try.of(()-> client.update(updateRequest, DEFAULT))
                .onFailure(err -> log.error("Feil vid skrivning til indeks vid profilering melding", err));
    }
}
