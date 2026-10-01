package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.oppfolging.domene.OppfolgingData;
import no.nav.pto.veilarbportefolje.oppfolging.dto.ManuellStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Service
@RequiredArgsConstructor
public class ManuellStatusService extends KafkaCommonNonKeyedConsumerService<ManuellStatusDTO> {
    private final OppfolgingClient oppfolgingClient;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;

    private static final Logger log = LoggerFactory.getLogger(ManuellStatusService.class);

    public void behandleKafkaMeldingLogikk(ManuellStatusDTO dto) {
        try {
            behandleManuellStatus(dto);
        } catch (BrukerIkkeUnderOppfolgingEnnaException e) {
            log.warn(e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Feil ved behandling av manuell status-melding", e);
            throw e;
        }
    }

    private void behandleManuellStatus(ManuellStatusDTO dto) {
        final AktorId aktorId = AktorId.of(dto.getAktorId());

        oppfolgingRepositoryV2.settManuellStatus(aktorId, dto.isErManuell());
        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktorId, dto);

        String manuellStatus = dto.isErManuell() ? ManuellBrukerStatus.MANUELL.name() : null;
        opensearchIndexerPaDatafelt.settManuellStatus(aktorId, manuellStatus);
        secureLog.info("Oppdatert manuellstatus for bruker {}, ny status: {}", aktorId, manuellStatus);
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, ManuellStatusDTO dto) {
        if (hentManuellStatus(aktorId) == dto.isErManuell()) {
            return;
        }
        boolean erUnderOppfolgingIVeilarboppfolging = oppfolgingClient.hentUnderOppfolging(aktorId);
        if (erUnderOppfolgingIVeilarboppfolging) {
            throw new BrukerIkkeUnderOppfolgingEnnaException("Fikk 'manuell status melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private boolean hentManuellStatus(AktorId aktoerId) {
        return oppfolgingRepositoryV2.hentOppfolgingData(aktoerId)
                .map(OppfolgingData::getManuell)
                .orElse(false);
    }
}
