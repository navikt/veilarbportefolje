package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Service
@RequiredArgsConstructor
public class VeilederTilordnetService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ArbeidslisteService arbeidslisteService;
    private final UnleashService unleashService;
    private final ElasticServiceV2 elasticServiceV2;

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final VeilederTilordnetDTO dto = JsonUtils.fromJson(kafkaMelding, VeilederTilordnetDTO.class);
        final AktorId aktoerId = dto.getAktorId();

        oppfolgingRepository.settVeileder(aktoerId, dto.getVeilederId());
        if (erPostgresPa(unleashService)) {
            oppfolgingRepositoryV2.settVeileder(aktoerId, dto.getVeilederId());
        }

        elasticServiceV2.oppdaterVeileder(aktoerId, dto.getVeilederId());

        final boolean harByttetNavKontor = arbeidslisteService.brukerHarByttetNavKontor(aktoerId);
        if (harByttetNavKontor) {
            arbeidslisteService.slettArbeidsliste(aktoerId);
        }
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
