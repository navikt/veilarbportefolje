package no.nav.pto.veilarbportefolje.cv;

import no.nav.arbeid.cv.avro.Melding;
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

import static no.nav.metrics.MetricsFactory.createEvent;

public class CvService implements KafkaConsumerService<Melding> {

    private final RegistreringService registreringService;
    private final BrukerRepository brukerRepository;
    private final ElasticIndexer elasticIndexer;

    public CvService(RegistreringService registreringService, BrukerRepository brukerRepository, ElasticIndexer elasticIndexer) {
        this.registreringService = registreringService;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    public void behandleKafkaMelding(Melding melding) {
        AktoerId aktoerId = AktoerId.of(melding.getAktoerId());
        Optional<ZonedDateTime> registreringOpprettet = registreringService.hentRegistreringOpprettet(aktoerId);
        if (!harDeltCvMedNav(melding.getSistEndret(), registreringOpprettet)) {
            createEvent("portefolje_har_ikke_delt_cv").report();
            return;
        }

        createEvent("portefolje_har_delt_cv").report();
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
