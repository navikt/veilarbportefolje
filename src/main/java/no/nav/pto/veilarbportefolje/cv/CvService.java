package no.nav.pto.veilarbportefolje.cv;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.util.Result;

import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.metrics.MetricsFactory.createEvent;

@Slf4j
public class CvService implements KafkaConsumerService<Melding> {

    private final BrukerRepository brukerRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elasticIndexer;

    public CvService(
            BrukerRepository brukerRepository,
            OppfolgingRepository oppfolgingRepository,
            ElasticIndexer elasticIndexer
    ) {
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    @Override
    public void behandleKafkaMelding(Melding melding) {
        if (melding.getMeldingstype() == Meldingstype.SLETT) {
            return;
        }

        AktoerId aktoerId = AktoerId.of(melding.getAktoerId());

        Result<Timestamp> result = oppfolgingRepository.hentStartdatoForOppfolging(aktoerId);
        if (result.isErr() || result.isEmpty()) {
            log.info("Kunne ikke hente startdato for oppfølging for bruker {}", aktoerId);
            return;
        }

        Instant oppfolgingStartet = result.orElseThrowException().toInstant();
        Instant cvSistEndret = melding.getSistEndret().toDate().toInstant();

        log.info("Bruker {} startet oppfølging {} og endret sist cv {}", aktoerId.aktoerId, oppfolgingStartet, cvSistEndret);

        if (!harDeltCvMedNav(oppfolgingStartet, cvSistEndret)) {
            log.info("Bruker {} har ikke delt cv med nav", aktoerId);
            createEvent("portefolje_har_ikke_delt_cv").report();
            return;
        }

        log.info("Bruker {} har delt cv med nav", aktoerId.aktoerId);
        createEvent("portefolje_har_delt_cv").report();
        brukerRepository.setHarDeltCvMedNav(aktoerId, true).orElseThrowException();
        elasticIndexer.indekser(aktoerId).orElseThrowException();
    }

    static boolean harDeltCvMedNav(Instant oppfolgingStartet, Instant cvSistEndret) {
        return oppfolgingStartet.isBefore(cvSistEndret);
    }

    public Result<Integer> setHarDeltCvTilNei(AktoerId aktoerId) {
        return brukerRepository.setHarDeltCvMedNav(aktoerId, false);
    }
}
