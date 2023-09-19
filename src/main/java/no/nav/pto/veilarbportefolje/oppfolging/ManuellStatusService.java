package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class ManuellStatusService extends KafkaCommonConsumerService<ManuellStatusDTO> {
    private final OppfolgingService oppfolgingService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @SneakyThrows
    public void behandleKafkaMeldingLogikk(ManuellStatusDTO dto) {
        final AktorId aktorId = AktorId.of(dto.getAktorId());

        oppfolgingRepositoryV2.settManuellStatus(aktorId, dto.isErManuell());
        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktorId, dto);

        String manuellStatus = dto.isErManuell() ? ManuellBrukerStatus.MANUELL.name() : null;
        opensearchIndexerV2.settManuellStatus(aktorId, manuellStatus);
        secureLog.info("Oppdatert manuellstatus for bruker {}, ny status: {}", aktorId, manuellStatus);
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, ManuellStatusDTO dto) throws IOException {
        if (hentManuellStatus(aktorId) == dto.isErManuell()) {
            return;
        }
        boolean erUnderOppfolgingIVeilarboppfolging = oppfolgingService.hentUnderOppfolging2(aktorId);
        if (erUnderOppfolgingIVeilarboppfolging) {
            throw new IllegalStateException("Fikk 'manuell status melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private boolean hentManuellStatus(AktorId aktoerId) {
        return oppfolgingRepositoryV2.hentOppfolgingData(aktoerId)
                .map(BrukerOppdatertInformasjon::getManuell)
                .orElse(false);
    }
}
