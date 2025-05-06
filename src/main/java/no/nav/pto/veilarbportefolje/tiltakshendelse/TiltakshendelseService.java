package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltakshendelseService extends KafkaCommonNonKeyedConsumerService<KafkaTiltakshendelse> {
    private final TiltakshendelseRepository repository;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Override
    protected void behandleKafkaMeldingLogikk(KafkaTiltakshendelse tiltakshendelseData) {

        Optional<AktorId> aktorId = brukerServiceV2.hentAktorId(tiltakshendelseData.fnr());
        if (aktorId.isEmpty()) {
            if (isDevelopment().orElse(false)) {
                return;
            }

            throw new RuntimeException("Kunne ikke hente aktørid for fnr");
        }

        if (Boolean.TRUE.equals(tiltakshendelseData.aktiv())) {
            behandleAktivHendelse(tiltakshendelseData, aktorId.get());
        } else {
            behandleInktivHendelse(tiltakshendelseData, aktorId.get());
        }
    }

    private void behandleAktivHendelse(KafkaTiltakshendelse tiltakshendelseData, AktorId aktorId) {
        boolean erEldsteTiltakshendelse = repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelseData);

        if (erEldsteTiltakshendelse) {
            opensearchIndexerV2.updateTiltakshendelse(aktorId, KafkaTiltakshendelse.mapTilTiltakshendelse(tiltakshendelseData));
        }
    }

    private void behandleInktivHendelse(KafkaTiltakshendelse tiltakshendelseData, AktorId aktorId) {
        Tiltakshendelse eldsteTiltakshendelse = repository.slettTiltakshendelseOgHentEldste(tiltakshendelseData.id(), tiltakshendelseData.fnr());

        if (eldsteTiltakshendelse != null) {
            opensearchIndexerV2.updateTiltakshendelse(aktorId, eldsteTiltakshendelse);
        } else {
            opensearchIndexerV2.slettTiltakshendelse(aktorId);
        }
    }
}
