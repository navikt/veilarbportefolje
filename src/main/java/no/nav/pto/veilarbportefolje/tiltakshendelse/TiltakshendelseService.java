package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltakshendelseService extends KafkaCommonConsumerService<KafkaTiltakshendelse> {
    private final TiltakshendelseRepository repository;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Override
    protected void behandleKafkaMeldingLogikk(KafkaTiltakshendelse tiltakshendelseData) {
        AktorId aktorId = brukerServiceV2.hentAktorId(tiltakshendelseData.fnr())
                .orElseThrow(() -> new RuntimeException("Kunne ikke hente aktørid for fnr"));

        boolean erUnderOppfolgingIVeilarboppfolging = oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktorId);
        if (!erUnderOppfolgingIVeilarboppfolging) {
            secureLog.warn("Fikk en tiltakshendelse på en bruker som ikke er under oppfølging i veilarbportefolje, sjekk om det er en person som har byttet ident eller om denne hendelsen er på en person som ikke lenger er under oppfølging. Fnr: {}", tiltakshendelseData.fnr());
            throw new RuntimeException("Tiltakshendelse på en bruker som ikke er under oppfølging i veilarbportefolje");
        }

        if (Boolean.TRUE.equals(tiltakshendelseData.aktiv())) {
            behandleAktivHendelse(tiltakshendelseData, aktorId);
        } else {
            behandleInktivHendelse(tiltakshendelseData, aktorId);
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
