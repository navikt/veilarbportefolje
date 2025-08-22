package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class VeilederSistTilordnetService extends KafkaCommonNonKeyedConsumerService<VeilederSistTilordnetDTO> {
    private final OppfolgingService oppfolgingService;
    private final OppfolgingRepositoryV3 oppfolgingRepositoryV3;
    //private final HuskelappService huskelappService;
    //private final FargekategoriService fargekategoriService;
    //private final OpensearchIndexerV2 opensearchIndexerV2;
    //private final PdlIdentRepository pdlIdentRepository;

    @Override
    public void behandleKafkaMeldingLogikk(VeilederSistTilordnetDTO dto) {
        final AktorId aktoerId = dto.getAktorId();
        final VeilederId veilederId = dto.getVeilederId();
        final ZonedDateTime sistTilordnet = dto.getTilordnet();

        lagreSistTilordnetVeileder(aktoerId, veilederId, sistTilordnet);
    }

    public void lagreSistTilordnetVeileder(AktorId aktoerId, VeilederId veilederId, ZonedDateTime sistTilordnet) {
        oppfolgingRepositoryV3.settVeileder(aktoerId, veilederId);
        oppfolgingRepositoryV3.settSistTilordnetDato(aktoerId, sistTilordnet);

        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktoerId, veilederId);
        //opensearchIndexerV2.oppdaterVeileder(aktoerId, veilederId, sistTilordnet);
        secureLog.info("Oppdatert bruker: {}, til veileder med id: {}", aktoerId, veilederId);

//        Optional<Fnr> maybeFnr = Optional.ofNullable(pdlIdentRepository.hentFnrForAktivBruker(aktoerId));
//
//        final boolean brukerHarByttetNavkontorHuskelapp = huskelappService.brukerHarHuskelappPaForrigeNavkontor(aktoerId, maybeFnr);
//        if (brukerHarByttetNavkontorHuskelapp) {
//            huskelappService.deaktivereAlleHuskelapperPaaBruker(aktoerId, maybeFnr);
//        }
//
//        final boolean brukerHarByttetNavkontorFargekategori = fargekategoriService.brukerHarFargekategoriPaForrigeNavkontor(aktoerId, maybeFnr);
//        if (brukerHarByttetNavkontorFargekategori) {
//            fargekategoriService.slettFargekategoriPaaBruker(aktoerId, maybeFnr);
//        }
    }

    private void kastErrorHvisBrukerSkalVaereUnderOppfolging(AktorId aktorId, VeilederId veilederId) {
        if (hentVeileder(aktorId).equals(veilederId)) {
            return;
        }
        boolean erUnderOppfolgingIVeilarboppfolging = oppfolgingService.hentUnderOppfolging(aktorId);
        if (erUnderOppfolgingIVeilarboppfolging) {
            throw new IllegalStateException("Fikk 'veileder melding' på bruker som enda ikke er under oppfølging i veilarbportefolje");
        }
    }

    private VeilederId hentVeileder(AktorId aktoerId) {
        return oppfolgingRepositoryV3.hentVeilederForBruker(aktoerId)
                .orElse(VeilederId.of(null));
    }
}
