package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

@Service
public class OppfolgingAvsluttetService implements KafkaConsumerService<String> {

    private final ArbeidslisteService arbeidslisteService;
    private final OppfolgingRepository oppfolgingRepository;
    private final RegistreringService registreringService;
    private final ElasticServiceV2 elasticServiceV2;
    private final SisteEndringService sisteEndringService;

    public OppfolgingAvsluttetService(ArbeidslisteService arbeidslisteService,
                                      OppfolgingRepository oppfolgingRepository,
                                      RegistreringService registreringService,
                                      ElasticServiceV2 elasticServiceV2,
                                      SisteEndringService sisteEndringService) {
        this.arbeidslisteService = arbeidslisteService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.registreringService = registreringService;
        this.elasticServiceV2 = elasticServiceV2;
        this.sisteEndringService = sisteEndringService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final OppfolgingAvsluttetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingAvsluttetDTO.class);
        final AktoerId aktoerId = dto.getAktorId();

        oppfolgingRepository.slettOppfolgingData(aktoerId);
        registreringService.slettRegistering(aktoerId);
        arbeidslisteService.slettArbeidsliste(aktoerId);
        sisteEndringService.slettSisteEndringer(aktoerId);
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
