package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import org.springframework.stereotype.Service;

@Service
public class OppfolgingAvsluttetService implements KafkaConsumerService<String> {

    private final ArbeidslisteService arbeidslisteService;
    private final OppfolgingRepository oppfolgingRepository;
    private final RegistreringService registreringService;
    private final ElasticServiceV2 elasticServiceV2;

    public OppfolgingAvsluttetService(ArbeidslisteService arbeidslisteService,
                                      OppfolgingRepository oppfolgingRepository,
                                      RegistreringService registreringService,
                                      ElasticServiceV2 elasticServiceV2) {
        this.arbeidslisteService = arbeidslisteService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.registreringService = registreringService;
        this.elasticServiceV2 = elasticServiceV2;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final OppfolgingAvsluttetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingAvsluttetDTO.class);
        final AktoerId aktoerId = dto.getAktorId();

        oppfolgingRepository.settOppfolgingTilFalse(aktoerId);
        registreringService.slettRegistering(aktoerId);
        arbeidslisteService.slettArbeidsliste(aktoerId);
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
