package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.oppfolging.response.Veilarbportefoljeinfo;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ManuellStatusService extends KafkaCommonConsumerService<ManuellStatusDTO> {
    private final OppfolgingService oppfolgingService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ElasticServiceV2 elasticServiceV2;

    public void behandleKafkaMeldingLogikk(ManuellStatusDTO dto) {
        final AktorId aktorId = AktorId.of(dto.getAktorId());

        oppfolgingRepository.settManuellStatus(aktorId, dto.isErManuell());
        oppfolgingRepositoryV2.settManuellStatus(aktorId, dto.isErManuell());

        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktorId, dto);

        String manuellStatus = dto.isErManuell() ? ManuellBrukerStatus.MANUELL.name() : null;
        elasticServiceV2.settManuellStatus(aktorId, manuellStatus);
        log.info("Oppdatert manuellstatus for bruker {}, ny status: {}", aktorId, manuellStatus);
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, ManuellStatusDTO dto) {
        if (hentManuellStatus(aktorId) == dto.isErManuell()) {
            return;
        }
        Optional<Veilarbportefoljeinfo> oppfolgingsdata = oppfolgingService.hentOppfolgingsDataFraVeilarboppfolging(aktorId);
        if (oppfolgingsdata.isPresent() && oppfolgingsdata.get().isErUnderOppfolging()) {
            throw new IllegalStateException("Fikk 'manuell status melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private boolean hentManuellStatus(AktorId aktoerId) {
        return oppfolgingRepository.hentOppfolgingData(aktoerId).map(BrukerOppdatertInformasjon::getManuell).orElse(false);
    }
}
