package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltakshendelseService extends KafkaCommonNonKeyedConsumerService<KafkaTiltakshendelse> {
    private final TiltakshendelseRepository repository;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final TiltakshendelseCleanupService tiltakshendelseCleanupService;

    @Override
    protected void behandleKafkaMeldingLogikk(KafkaTiltakshendelse tiltakshendelseData) {

        AktorId aktorId = brukerServiceV2.hentAktorId(tiltakshendelseData.fnr())
                .orElseGet(() -> {
                    // Slett før vi feiler, og gjør det i egen transaksjon så det ikke rulles tilbake
                    tiltakshendelseCleanupService.slettConsumerRecordOgTiltakshendelse(
                            tiltakshendelseData.id(),
                            tiltakshendelseData.fnr().toString()
                    );
                    throw new RuntimeException("Kunne ikke hente aktørid for fnr");
                });

        if (Boolean.TRUE.equals(tiltakshendelseData.aktiv())) {
            behandleAktivHendelse(tiltakshendelseData, aktorId);
        } else {
            behandleInktivHendelse(tiltakshendelseData, aktorId);
        }
    }

    private void behandleAktivHendelse(KafkaTiltakshendelse tiltakshendelseData, AktorId aktorId) {
        boolean erEldsteTiltakshendelse = repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelseData);

        if (erEldsteTiltakshendelse) {
            opensearchIndexerPaDatafelt.updateTiltakshendelse(aktorId, KafkaTiltakshendelse.mapTilTiltakshendelse(tiltakshendelseData));
        }
    }

    private void behandleInktivHendelse(KafkaTiltakshendelse tiltakshendelseData, AktorId aktorId) {
        Tiltakshendelse eldsteTiltakshendelse = repository.slettTiltakshendelseOgHentEldste(tiltakshendelseData.id(), tiltakshendelseData.fnr());

        if (eldsteTiltakshendelse != null) {
            opensearchIndexerPaDatafelt.updateTiltakshendelse(aktorId, eldsteTiltakshendelse);
        } else {
            opensearchIndexerPaDatafelt.slettTiltakshendelse(aktorId);
        }
    }
}
