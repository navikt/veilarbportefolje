package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;

public class RegistreringService {
    private RegistreringRepository registreringRepository;
    private ElasticIndexer elasticIndexer;

    public RegistreringService(RegistreringRepository registreringRepository, ElasticIndexer elasticIndexer) {
        this.registreringRepository = registreringRepository;
        this.elasticIndexer = elasticIndexer;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        registreringRepository.insertBrukerRegistrering(kafkaRegistreringMelding);
        AktoerId aktoerId = AktoerId.of(kafkaRegistreringMelding.getAktorid());
        elasticIndexer.indekserAsynkront(aktoerId);
    }

}
