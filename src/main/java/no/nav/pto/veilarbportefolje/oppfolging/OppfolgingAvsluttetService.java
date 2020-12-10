package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.KAFKA_OPPFOLGING;

@Service
public class OppfolgingAvsluttetService implements KafkaConsumerService<String> {

    private final ArbeidslisteService arbeidslisteService;
    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final UnleashService unleashService;

    public OppfolgingAvsluttetService(ArbeidslisteService arbeidslisteService,
                                      OppfolgingRepository oppfolgingRepository,
                                      ElasticServiceV2 elasticServiceV2, UnleashService unleashService) {
        this.arbeidslisteService = arbeidslisteService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.elasticServiceV2 = elasticServiceV2;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if (!unleashService.isEnabled(KAFKA_OPPFOLGING)) {
            return;
        }

        final OppfolgingAvsluttetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingAvsluttetDTO.class);
        final AktoerId aktoerId = dto.getAktorId();

        arbeidslisteService.slettArbeidsliste(aktoerId);
        oppfolgingRepository.settOppfolgingTilFalse(aktoerId);
        elasticServiceV2.markerBrukerSomSlettet(aktoerId);
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
