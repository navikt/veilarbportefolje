package no.nav.pto.veilarbportefolje.cv;

import io.micrometer.core.instrument.Counter;
import no.nav.arbeid.cv.avro.Cv;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.Result;
import org.joda.time.DateTime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;

public class CvService implements KafkaConsumerService<Cv> {

    private final RegistreringService registreringService;
    private final BrukerRepository brukerRepository;
    private final ElasticIndexer elasticIndexer;
    private final Counter antallBrukereSomIkkeHarDeltCv;
    private final Counter antallBrukereSomHarDeltCv;

    public CvService(RegistreringService registreringService, BrukerRepository brukerRepository, ElasticIndexer elasticIndexer) {
        this.registreringService = registreringService;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        antallBrukereSomIkkeHarDeltCv = Counter.builder("portefolje_har_ikke_delt_cv").register(getMeterRegistry());
        antallBrukereSomHarDeltCv = Counter.builder("portefolje_har_delt_cv").register(getMeterRegistry());
    }

    @Override
    public void behandleKafkaMelding(Cv cv) {
        AktoerId aktoerId = AktoerId.of(cv.getAktoerId());
        Optional<ZonedDateTime> registreringOpprettet = registreringService.hentRegistreringOpprettet(aktoerId);
        if (!harDeltCvMedNav(cv.getSistEndret(), registreringOpprettet)) {
            antallBrukereSomIkkeHarDeltCv.increment();
            return;
        }

        antallBrukereSomHarDeltCv.increment();
        brukerRepository.setHarDeltCvMedNav(aktoerId, true).orElseThrowException();
        elasticIndexer.indekser(aktoerId).orElseThrowException();
    }

    static boolean harDeltCvMedNav(DateTime sistEndret, Optional<ZonedDateTime> registreringOpprettet) {
        long unixTime = sistEndret.toInstant().getMillis();
        ZonedDateTime cvSistEndret = Instant.ofEpochMilli(unixTime).atZone(ZoneId.of("Europe/Oslo"));

        return registreringOpprettet
                .map(registeringsDato -> registeringsDato.isBefore(cvSistEndret))
                .orElse(false);
    }

    public Result<Integer> setHarDeltCvTilNei(AktoerId aktoerId) {
        return brukerRepository.setHarDeltCvMedNav(aktoerId, false);
    }
}
