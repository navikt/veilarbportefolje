package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.util.DateUtils;

public class ProfileringService implements KafkaConsumerService<ArbeidssokerProfilertEvent> {
    private ProfileringRepository profileringRepository;
    private ElasticIndexer elasticIndexer;

    public ProfileringService(ProfileringRepository profileringRepository, ElasticIndexer elasticIndexer) {
        this.profileringRepository = profileringRepository;
        this.elasticIndexer = elasticIndexer;
    }

    public void behandleKafkaMelding (ArbeidssokerProfilertEvent kafkaMelding) {

        ArbeidssokerProfilertEvent brukerProfilering = profileringRepository.hentBrukerProfilering(AktoerId.of(kafkaMelding.getAktorid()));
        if(brukerProfilering!= null) {
            if(DateUtils.dateIfAfterAnotherDate(kafkaMelding.getProfileringGjennomfort(), brukerProfilering.getProfileringGjennomfort())) {
                profileringRepository.updateBrukerProfilering(kafkaMelding);
                elasticIndexer.indekser(AktoerId.of(kafkaMelding.getAktorid()));
            }
        } else {
            profileringRepository.insertBrukerProfilering(kafkaMelding);
            elasticIndexer.indekser(AktoerId.of(kafkaMelding.getAktorid()));

        }

    }

}
