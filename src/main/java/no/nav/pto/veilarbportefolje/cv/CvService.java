package no.nav.pto.veilarbportefolje.cv;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.json.JsonUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.pto.veilarbportefolje.cv.CvService.Ressurs.CV_SAMTYKKE;

@Slf4j
public class CvService implements KafkaConsumerService<String> {

    private final BrukerRepository brukerRepository;
    private final ElasticIndexer elasticIndexer;

    public CvService(
            BrukerRepository brukerRepository,
            ElasticIndexer elasticIndexer
    ) {
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
    }

    enum Ressurs {
        CV_SAMTYKKE
    }

    enum MeldingType {
        SAMTYKKE_OPPRETTET,
        SAMTYKKE_SLETTET
    }

    @Value
    static class Melding {
        AktoerId aktorId;
        Fnr fnr;
        MeldingType meldingType;
        Ressurs ressurs;
    }

    @Override
    public void behandleKafkaMelding(String payload) {
        Melding melding = fromJson(payload, Melding.class);
        AktoerId aktorId = melding.getAktorId();

        if (melding.getRessurs() != CV_SAMTYKKE) {
            log.info("Ignorer melding for ressurs {} for bruker {}", melding.getRessurs(), aktorId);
            return;
        }

        switch (melding.meldingType) {
            case SAMTYKKE_OPPRETTET:
                log.info("Bruker {} har delt cv med nav", aktorId);
                brukerRepository.setHarDeltCvMedNav(aktorId, true).orElseThrowException();
                createEvent("portefolje_har_delt_cv").report();
                elasticIndexer.indekser(aktorId);
                break;
            case SAMTYKKE_SLETTET:
                brukerRepository.setHarDeltCvMedNav(aktorId, false).orElseThrowException();
                elasticIndexer.indekser(aktorId);
                break;
            default:
                log.info("Ignorer melding av type {} for bruker {}", melding.getMeldingType(), aktorId);
        }

    }
}
