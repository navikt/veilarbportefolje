package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.interfaces.HandtereOppfolgingData;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.registrering.endring.EndringIRegistreringService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingAvsluttetService {
    private final ArbeidslisteService arbeidslisteService;
    private final HuskelappService huskelappService;
    private final FargekategoriService fargekategoriService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final RegistreringService registreringService;
    private final EndringIRegistreringService endringIRegistreringService;
    private final CVService cvService;
    private final PdlService pdlService;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final SisteEndringService sisteEndringService;
    private final Siste14aVedtakService siste14aVedtakService;
    private final EnsligeForsorgereService ensligeForsorgereService;
    private final ProfileringService profileringService;
    private final OppfolgingsbrukerServiceV2 oppfolgingsbrukerService;
    private final DialogService dialogService;
    private final GruppeAktivitetService gruppeAktivitetService;
    private final SkjermingService skjermingService;
    private final YtelsesService ytelsesService;
    private final TiltakService tiltakService;
    private final AktivitetService aktivitetService;

    @Transactional
    public void avsluttOppfolging(AktorId aktoerId) {
        List<AktorId> alleAktoerIdForBruker = pdlService.hentAlleAktoerForBruker(aktoerId);
        List<Fnr> alleFolkeregisterIdenterForBruker = pdlService.hentAlleFolkeregistreIdenterForBruker(aktoerId);

        List<HandtereOppfolgingData<AktorId>> oppfolgingServicesAktorId
                = List.of(oppfolgingRepositoryV2, registreringService, endringIRegistreringService, arbeidslisteService,
                sisteEndringService, cvService, siste14aVedtakService, profileringService, dialogService,
                gruppeAktivitetService, ytelsesService, tiltakService, aktivitetService, pdlService);

        List<HandtereOppfolgingData<Fnr>> oppfolgingServicesFnr = List.of(huskelappService, fargekategoriService,
                oppfolgingsbrukerService, skjermingService, ensligeForsorgereService);


        alleAktoerIdForBruker.forEach((aktorId) -> oppfolgingServicesAktorId.forEach(x -> x.slettOppfolgingData(aktorId)));

        alleFolkeregisterIdenterForBruker.forEach(folkeregisterIdent -> oppfolgingServicesFnr.forEach(x -> x.slettOppfolgingData(folkeregisterIdent)));

        opensearchIndexerV2.slettDokumenter(alleAktoerIdForBruker);
        secureLog.info(
                "Bruker: {} har avsluttet oppfølging og er slettet. Slettet også data for historiske identer {} for samme bruker.",
                aktoerId,
                String.join(", ", alleAktoerIdForBruker.stream().map(AktorId::get).toList())
        );
    }
}
