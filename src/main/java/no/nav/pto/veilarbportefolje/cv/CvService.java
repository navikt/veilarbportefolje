package no.nav.pto.veilarbportefolje.cv;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.util.Result;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.pto.veilarbportefolje.cv.CvService.Ressurs.CV_HJEMMEL;

@Slf4j
public class CvService implements KafkaConsumerService<String> {

    private final BrukerRepository brukerRepository;
    private final ElasticServiceV2 elasticServiceV2;

    public CvService(
            BrukerRepository brukerRepository,
            ElasticServiceV2 elasticServiceV2
    ) {
        this.brukerRepository = brukerRepository;
        this.elasticServiceV2 = elasticServiceV2;
    }

    enum Ressurs {
        CV_HJEMMEL,
        CV_GENERELL,
        ARBEIDSGIVER_GENERELL
    }

    enum MeldingType {
        SAMTYKKE_OPPRETTET,
        SAMTYKKE_SLETTET
    }

    @Value
    static class Melding {
        AktoerId aktoerId;
        Fnr fnr;
        MeldingType meldingType;
        Ressurs ressurs;
    }

    @Override
    public void behandleKafkaMelding(String payload) {
        Melding melding = fromJson(payload, Melding.class);
        AktoerId aktorId = melding.getAktoerId();

        if (melding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", melding.getRessurs(), aktorId);
            return;
        }

        switch (melding.meldingType) {
            case SAMTYKKE_OPPRETTET:
                log.info("Bruker {} har delt cv med nav", aktorId);
                brukerRepository.setHarDeltCvMedNav(aktorId, true).orElseThrowException();
                createEvent("portefolje_har_delt_cv").report();
                elasticServiceV2.updateHarDeltCv(melding.getFnr(), true);
                break;
            case SAMTYKKE_SLETTET:
                log.info("Bruker {} har ikke delt cv med nav", aktorId);
                brukerRepository.setHarDeltCvMedNav(aktorId, false).orElseThrowException();
                createEvent("portefolje_har_ikke_delt_cv").report();
                elasticServiceV2.updateHarDeltCv(melding.getFnr(), false);
                break;
            default:
                log.info("Ignorer melding av type {} for bruker {}", melding.getMeldingType(), aktorId);
        }
    }

    public Result<Integer> setHarDeltCvTilNei(AktoerId aktoerId) {
        return brukerRepository.setHarDeltCvMedNav(aktoerId, false);
    }
}
