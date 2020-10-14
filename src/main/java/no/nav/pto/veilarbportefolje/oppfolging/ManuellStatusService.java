package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.krr.DkifClient;
import no.nav.pto.veilarbportefolje.krr.DkifKontaktinfoDTO;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.KAFKA_OPPFOLGING;

@Service
public class ManuellStatusService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final DkifClient dkifClient;
    private final UnleashService unleashService;

    public ManuellStatusService(OppfolgingRepository oppfolgingRepository, ElasticServiceV2 elasticServiceV2, DkifClient dkifClient, UnleashService unleashService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.dkifClient = dkifClient;
        this.elasticServiceV2 = elasticServiceV2;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if (!unleashService.isEnabled(KAFKA_OPPFOLGING)) {
            return;
        }

        final ManuellStatusDTO dto = JsonUtils.fromJson(kafkaMelding, ManuellStatusDTO.class);
        final AktoerId aktoerId = AktoerId.of(dto.getAktorId());

        oppfolgingRepository.settManuellStatus(aktoerId, dto.isErManuell());

        boolean reservert = dkifClient
                .hentKontaktInfo(aktoerId)
                .map(DkifKontaktinfoDTO::isReservert)
                .orElse(true);

        final ManuellBrukerStatus manuellBrukerStatus = utledManuellBrukerStatus(reservert);
        elasticServiceV2.settManuellStatus(aktoerId, manuellBrukerStatus);
    }

    public static ManuellBrukerStatus utledManuellBrukerStatus(boolean reservert) {
        return reservert ? ManuellBrukerStatus.KRR : ManuellBrukerStatus.MANUELL;
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
