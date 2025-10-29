package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.oppfolging.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.oppfolging.dto.NyForVeilederDTO;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class NyForVeilederService extends KafkaCommonNonKeyedConsumerService<NyForVeilederDTO> {
    private final OppfolgingClient oppfolgingClient;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;

    @Override
    protected void behandleKafkaMeldingLogikk(NyForVeilederDTO dto) {
        AktorId aktorId = dto.getAktorId();
        final boolean brukerErNyForVeileder = dto.isNyForVeileder();
        oppfolgingRepositoryV2.settNyForVeileder(aktorId, brukerErNyForVeileder);

        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktorId, brukerErNyForVeileder);

        opensearchIndexerPaDatafelt.oppdaterNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);
        secureLog.info("Oppdatert bruker: {}, er ny for veileder: {}", dto.getAktorId(), brukerErNyForVeileder);
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, boolean nyForVeileder) {
        if (hentNyForVeileder(aktorId) == nyForVeileder) {
            return;
        }
        boolean erUnderOppfolgingIVeilarboppfolging = oppfolgingClient.hentUnderOppfolging(aktorId);
        if (erUnderOppfolgingIVeilarboppfolging) {
            throw new IllegalStateException("Fikk 'ny for veiledere melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private boolean hentNyForVeileder(AktorId aktoerId) {
        return oppfolgingRepositoryV2.hentOppfolgingData(aktoerId)
                .map(BrukerOppdatertInformasjon::getNyForVeileder)
                .orElse(false);
    }
}
