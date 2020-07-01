package no.nav.pto.veilarbportefolje.cv;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.cv.CvService.Ressurs.CV_HJEMMEL;

@Slf4j
@Service
public class CvService implements KafkaConsumerService<String> {

    private final BrukerRepository brukerRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final MetricsClient metricsClient;

    @Autowired
    public CvService(
            BrukerRepository brukerRepository,
            ElasticServiceV2 elasticServiceV2,
            MetricsClient metricsClient
    ) {
        this.brukerRepository = brukerRepository;
        this.elasticServiceV2 = elasticServiceV2;
        this.metricsClient = metricsClient;
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

        if (melding.getFnr() == null) {
            if (EnvironmentUtils.isProduction().orElse(false)) {
                log.error("Bruker {} har ikke fnr i melding fra samtykke-topic", aktorId);
            }
            return;
        }

        switch (melding.meldingType) {
            case SAMTYKKE_OPPRETTET:
                log.info("Bruker {} har delt cv med nav", aktorId);
                brukerRepository.setHarDeltCvMedNav(aktorId, true).orElseThrowException();
                metricsClient.report(new Event("portefolje_har_delt_cv"));
                elasticServiceV2.updateHarDeltCv(melding.getFnr(), true);
                break;
            case SAMTYKKE_SLETTET:
                log.info("Bruker {} har ikke delt cv med nav", aktorId);
                brukerRepository.setHarDeltCvMedNav(aktorId, false).orElseThrowException();
                metricsClient.report(new Event("portefolje_har_ikke_delt_cv"));
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
