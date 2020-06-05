package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.Result;

import java.io.IOException;
import java.util.Optional;

public class ProfileringService implements KafkaConsumerService<ArbeidssokerProfilertEvent> {
    private ProfileringRepository profileringRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;

    public ProfileringService(ProfileringRepository profileringRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        this.profileringRepository = profileringRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
    }

    public void behandleKafkaMelding (ArbeidssokerProfilertEvent kafkaMelding) {
        profileringRepository.upsertBrukerProfilering(kafkaMelding);
        aktoerService.hentFnrFraAktorId(AktoerId.of(kafkaMelding.getAktorid()))
                    .onSuccess(tryFnr -> upsertBrukerIndeks(tryFnr, kafkaMelding));
    }

    private void upsertBrukerIndeks(Fnr fnr, ArbeidssokerProfilertEvent kafkaMelding) {
        OppfolgingsBruker oppfolgingsBruker = new OppfolgingsBruker()
                .setProfilering_resultat(kafkaMelding.getProfilertTil().name())
                .setFnr(fnr.getFnr());

        elasticIndexer.oppdaterBrukerDoc(oppfolgingsBruker);
    }
}
