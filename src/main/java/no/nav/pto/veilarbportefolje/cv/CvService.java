package no.nav.pto.veilarbportefolje.cv;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.pto.veilarbportefolje.cv.CvService.Ressurs.CV_HJEMMEL;

@Slf4j
public class CvService implements KafkaConsumerService<String> {
    private final ElasticServiceV2 elasticServiceV2;
    private final AktoerService aktoerService;
    private final AtomicBoolean rewind;

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

    public CvService(ElasticServiceV2 elasticServiceV2, AktoerService aktoerService) {
        this.elasticServiceV2 = elasticServiceV2;
        this.aktoerService = aktoerService;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);
    }

    @Override
    public void behandleKafkaMelding(String payload) {
        Melding melding = fromJson(payload, Melding.class);
        AktoerId aktorId = melding.getAktoerId();

        if (melding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", melding.getRessurs(), aktorId);
            return;
        }

        Fnr fnr = melding.getFnr() == null ? hentFnrFraAktoerTjenesten(melding.getAktoerId()) : melding.getFnr();

        switch (melding.meldingType) {
            case SAMTYKKE_OPPRETTET:
                log.info("Bruker {} har delt cv med nav", aktorId);
                createEvent("portefolje_har_delt_cv").report();
                elasticServiceV2.updateHarDeltCv(fnr, true);
                break;
            case SAMTYKKE_SLETTET:
                log.info("Bruker {} har ikke delt cv med nav", aktorId);
                createEvent("portefolje_har_ikke_delt_cv").report();
                elasticServiceV2.updateHarDeltCv(fnr, false);
                break;
            default:
                log.info("Ignorer melding av type {} for bruker {}", melding.getMeldingType(), aktorId);
        }
    }

    private Fnr hentFnrFraAktoerTjenesten(AktoerId aktoerId) {
        log.info("Henter fnr fra aktoertjenesten for bruker {}...", aktoerId);
        return aktoerService.hentFnrFraAktorId(aktoerId).getOrElseThrow(() -> new IllegalStateException());
    }
}
