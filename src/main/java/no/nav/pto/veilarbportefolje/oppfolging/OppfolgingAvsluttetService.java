package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aap.AapService;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerService;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssokerRegistreringRepositoryV2;
import no.nav.pto.veilarbportefolje.cv.CVServiceV2;
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerService;
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingAvsluttetService {
    private final HuskelappService huskelappService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final CVServiceV2 cvServiceV2;
    private final PdlService pdlService;
    private final OpensearchIndexer opensearchIndexer;
    private final SisteEndringService sisteEndringService;
    private final Siste14aVedtakService siste14aVedtakService;
    private final EnsligeForsorgereService ensligeForsorgereService;
    private final PdlIdentRepository pdlIdentRepository;
    private final FargekategoriService fargekategoriService;
    private final OppfolgingsbrukerServiceV2 oppfolgingsbrukerServiceV2;
    private final ArbeidssoekerService arbeidssoekerService;
    private final ArbeidssokerRegistreringRepositoryV2 arbeidssokerRegistreringRepositoryV2;
    private final AapService aapService;
    private final TiltakspengerService tiltakspengerService;
    private final DagpengerService dagpengerService;

    public void avsluttOppfolging(AktorId aktorId) {
        Optional<Fnr> maybeFnr = Optional.ofNullable(pdlIdentRepository.hentFnrForAktivBruker(aktorId));

        oppfolgingRepositoryV2.slettOppfolgingData(aktorId);
        arbeidssokerRegistreringRepositoryV2.slettBrukerRegistrering(aktorId);
        arbeidssokerRegistreringRepositoryV2.slettBrukerProfilering(aktorId);
        arbeidssokerRegistreringRepositoryV2.slettEndringIRegistrering(aktorId);
        huskelappService.sletteAlleHuskelapperPaaBruker(aktorId, maybeFnr);
        sisteEndringService.slettSisteEndringer(aktorId);
        siste14aVedtakService.slettSiste14aVedtak(aktorId.get());
        pdlService.slettPdlData(aktorId);
        ensligeForsorgereService.slettEnsligeForsorgereData(aktorId);
        fargekategoriService.slettFargekategoriPaaBruker(aktorId, maybeFnr);
        oppfolgingsbrukerServiceV2.slettOppfolgingsbruker(aktorId, maybeFnr);
        arbeidssoekerService.slettArbeidssoekerData(aktorId, maybeFnr);
        aapService.slettAapData(aktorId, maybeFnr);
        tiltakspengerService.slettTiltakspengerData(aktorId, maybeFnr);
        dagpengerService.slettDagpengerData(aktorId, maybeFnr);
        cvServiceV2.slettCvData(aktorId, maybeFnr);
        opensearchIndexer.slettDokumenter(List.of(aktorId));
        secureLog.info("Bruker: {} har avsluttet oppfølging og er slettet", aktorId);
    }
}
