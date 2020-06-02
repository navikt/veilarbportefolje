package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;

public class RegistreringService implements KafkaConsumerService<ArbeidssokerRegistrertEvent> {
    private RegistreringRepository registreringRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;
    private BrukerRepository brukerRepository;

    public RegistreringService(RegistreringRepository registreringRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService, BrukerRepository brukerRepository) {
        this.registreringRepository = registreringRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
        this.brukerRepository = brukerRepository;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        AktoerId aktoerId = AktoerId.of(kafkaRegistreringMelding.getAktorid());
        registreringRepository.upsertBrukerRegistrering(kafkaRegistreringMelding);
        aktoerService.hentFnrFraAktorId(aktoerId)
                .onSuccess(this::indekserBruker);
    }

    private void indekserBruker(Fnr fnr) {
        Result<OppfolgingsBruker> oppfolgingsBrukerResult = brukerRepository.hentBruker(fnr);
        if(UnderOppfolgingRegler.erUnderOppfolging(oppfolgingsBrukerResult)) {
            elasticIndexer.indekser(fnr);
        }
    }
}
