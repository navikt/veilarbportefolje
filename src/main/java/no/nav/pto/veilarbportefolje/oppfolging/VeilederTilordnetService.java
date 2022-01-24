package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.oppfolging.response.Veilarbportefoljeinfo;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class VeilederTilordnetService extends KafkaCommonConsumerService<VeilederTilordnetDTO> {
    private final OppfolgingService oppfolgingService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ArbeidslisteService arbeidslisteService;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Override
    public void behandleKafkaMeldingLogikk(VeilederTilordnetDTO dto) {
        final AktorId aktoerId = dto.getAktorId();
        final VeilederId veilederId = dto.getVeilederId();

        oppfolgingRepository.settVeileder(aktoerId, veilederId);
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId);

        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktoerId, veilederId);
        opensearchIndexerV2.oppdaterVeileder(aktoerId, veilederId);
        log.info("Oppdatert bruker: {}, til veileder med id: {}", aktoerId, veilederId);

        // TODO: Slett oracle basert kode naar vi er over paa postgres.
        final boolean harByttetNavKontorPostgres = arbeidslisteService.brukerHarByttetNavKontorPostgres(aktoerId);
        if (harByttetNavKontorPostgres) {
            arbeidslisteService.slettArbeidslistePostgres(aktoerId);
        }

        final boolean harByttetNavKontor = arbeidslisteService.brukerHarByttetNavKontorOracle(aktoerId);
        if (harByttetNavKontor) {
            arbeidslisteService.slettArbeidsliste(aktoerId);
        }
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, VeilederId veilederId) {
        if (hentVeileder(aktorId).equals(veilederId)) {
            return;
        }
        Optional<Veilarbportefoljeinfo> oppfolgingsdata = oppfolgingService.hentOppfolgingsDataFraVeilarboppfolging(aktorId);
        if (oppfolgingsdata.isPresent() && oppfolgingsdata.get().isErUnderOppfolging()) {
            throw new IllegalStateException("Fikk 'veileder melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private VeilederId hentVeileder(AktorId aktoerId) {
        return VeilederId.of(oppfolgingRepository.hentOppfolgingData(aktoerId).map(BrukerOppdatertInformasjon::getVeileder).orElse(null));
    }
}
