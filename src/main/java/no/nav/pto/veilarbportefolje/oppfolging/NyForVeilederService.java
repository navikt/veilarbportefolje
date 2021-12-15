package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.oppfolging.response.Veilarbportefoljeinfo;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class NyForVeilederService extends KafkaCommonConsumerService<NyForVeilederDTO> {

    private final OppfolgingService oppfolgingService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ElasticServiceV2 elasticServiceV2;

    @Override
    protected void behandleKafkaMeldingLogikk(NyForVeilederDTO dto) {
        AktorId aktorId = dto.getAktorId();
        final boolean brukerErNyForVeileder = dto.isNyForVeileder();
        oppfolgingRepository.settNyForVeileder(aktorId, brukerErNyForVeileder);
        oppfolgingRepositoryV2.settNyForVeileder(aktorId, brukerErNyForVeileder);

        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktorId, brukerErNyForVeileder);

        elasticServiceV2.oppdaterNyForVeileder(aktorId, brukerErNyForVeileder);
        log.info("Oppdatert bruker: {}, er ny for veileder: {}", aktorId, brukerErNyForVeileder);
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, boolean nyForVeileder) {
        if (hentNyForVeileder(aktorId) == nyForVeileder) {
            return;
        }
        Optional<Veilarbportefoljeinfo> oppfolgingsdata = oppfolgingService.hentOppfolgingsDataFraVeilarboppfolging(aktorId);
        if (oppfolgingsdata.isPresent() && oppfolgingsdata.get().isErUnderOppfolging()) {
            throw new IllegalStateException("Fikk 'ny for veiledere melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private boolean hentNyForVeileder(AktorId aktoerId) {
        return oppfolgingRepository.hentOppfolgingData(aktoerId).map(BrukerOppdatertInformasjon::getNyForVeileder).orElse(false);
    }
}
