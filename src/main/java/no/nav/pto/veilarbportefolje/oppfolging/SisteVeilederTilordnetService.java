package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto_schema.kafka.json.topic.SisteTilordnetVeilederV1;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class SisteVeilederTilordnetService extends KafkaCommonConsumerService<SisteTilordnetVeilederV1> {
    private final VeilederTilordnetService veilederTilordnetService;
    private final UnleashService unleashService;

    @Override
    public void behandleKafkaMeldingLogikk(SisteTilordnetVeilederV1 dto) {

        if (unleashService.isEnabled(FeatureToggle.SISTE_TILORDNET_VEILEDER)) {
            final AktorId aktoerId = AktorId.of(dto.getAktorId());
            final VeilederId veilederId = VeilederId.of(dto.getVeilederId());

            log.info("Sett siste tilordnet veileder for aktorId: " + aktoerId);
            veilederTilordnetService.tilordneVeileder(aktoerId, veilederId);
        }
    }
}
