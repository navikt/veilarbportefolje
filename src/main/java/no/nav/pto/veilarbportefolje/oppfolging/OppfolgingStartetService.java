package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aap.AapService;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerService;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingStartetService {
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;
    private final PdlService pdlService;
    private final Siste14aVedtakService siste14aVedtakService;
    private final OppfolgingsbrukerServiceV2 oppfolgingsbrukerServiceV2;
    private final ArbeidssoekerService arbeidssoekerService;
    private final EnsligeForsorgereService ensligeForsorgereService;
    private final AapService aapService;
    private final TiltakspengerService tiltakspengerService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final BrukerServiceV2 brukerServiceV2;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final HuskelappService huskelappService;
    private final FargekategoriService fargekategoriService;

    public void behandleOppfølgingStartetEllerKontorEndret(Fnr fnr, AktorId aktorId, ZonedDateTime oppfolgingStartetDate, NavKontor navKontor) {
        var oppfolgingsbruker = oppfolgingRepositoryV2.hentOppfolgingData(aktorId);
        if (oppfolgingsbruker.isPresent() && oppfolgingsbruker.get().getOppfolging()) {
            oppfolgingsbrukerRepositoryV3.settNavKontor(fnr.get(), navKontor);
            oppdaterEnhetVedKontorbytteHuskelappFargekategori(fnr, EnhetId.of(navKontor.getValue()));
            opensearchIndexer.indekser(aktorId);
        } else {
            startOppfolging(aktorId, oppfolgingStartetDate, navKontor);
        }
    }

    private void oppdaterEnhetVedKontorbytteHuskelappFargekategori(Fnr fnr, EnhetId enhetForBruker) {
        try {
            Optional<AktorId> aktorIdForBruker = brukerServiceV2.hentAktorId(fnr);
            aktorIdForBruker.ifPresent(aktorId -> {
                Optional<NavKontor> navKontorForBruker = brukerServiceV2.hentNavKontor(fnr);
                if (navKontorForBruker.isPresent() && !Objects.equals(navKontorForBruker.get().getValue(), enhetForBruker.get())) {
                    brukerServiceV2.hentVeilederForBruker(aktorId).ifPresent(veilederForBruker -> {
                        List<String> veiledereMedTilgangTilEnhet = veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(enhetForBruker);
                        boolean brukerBlirAutomatiskTilordnetVeileder = veiledereMedTilgangTilEnhet.contains(veilederForBruker.getValue());
                        if (brukerBlirAutomatiskTilordnetVeileder) {
                            fargekategoriService.oppdaterEnhetPaaFargekategori(fnr, enhetForBruker, veilederForBruker);
                            huskelappService.oppdaterEnhetPaaHuskelapp(fnr, enhetForBruker, veilederForBruker);
                        }
                    });
                }
            });
        } catch (Exception e) {
            secureLog.error("Kunne ikke oppdatere enhet på huskelapp eller fargekategori ved kontrobytte for bruker: " + fnr, e);
        }
    }

    // TODO: Dersom en eller flere av disse operasjonene feiler og kaster exception vil
    //  kafka-meldingen bli lagret og retryet. Dette kan resultere i at vi mellomlagrer data
    //  vi ikke skal. Mulig vi burde wrappe dette i en try-catch slik at vi kan rydde opp
    //  i catch-en.
    //  Dette vil også muligens bidra til å gjøre usikkerheten rundt hvordan vi håndterer
    //  scenarier der AktorID/Fnr blir historisk før startOppfolging fullfører.
    public void startOppfolging(AktorId aktorId, ZonedDateTime oppfolgingStartetDate, NavKontor navKontor) {
        pdlService.hentOgLagrePdlData(aktorId);
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartetDate);
        siste14aVedtakService.hentOgLagreSiste14aVedtak(aktorId);
        oppfolgingsbrukerServiceV2.hentOgLagreOppfolgingsbruker(aktorId, navKontor);
        arbeidssoekerService.hentOgLagreArbeidssoekerdataForBruker(aktorId);
        ensligeForsorgereService.hentOgLagreEnsligForsorgerDataFraApi(aktorId);
        aapService.hentOgLagreAapForBrukerVedOppfolgingStart(aktorId);
        tiltakspengerService.hentOgLagreTiltakspengerForBrukerVedOppfolgingStart(aktorId);

        opensearchIndexer.indekser(aktorId);

        secureLog.info("Bruker {} har startet oppfølging: {}", aktorId, oppfolgingStartetDate);
    }
}
