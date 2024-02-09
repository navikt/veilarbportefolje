package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.cv.CVRepositoryV2;
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.registrering.endring.EndringIRegistreringService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe;
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
    private final CVRepositoryV2 cvRepositoryV2;
    private final PdlService pdlService;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final SisteEndringService sisteEndringService;
    private final Siste14aVedtakService siste14aVedtakService;
    private final EnsligeForsorgereService ensligeForsorgereService;
    private final PdlIdentRepository pdlIdentRepository;
    private final ProfileringService profileringService;


    public void avsluttOppfolging(AktorId aktoerId) {
        List<PDLIdent> alleIdenterForBruker = pdlIdentRepository.hentAlleIdenterForAktorId(aktoerId);
        List<AktorId> alleAktoerIdForBruker = alleIdenterForBruker
                .stream()
                .filter(x -> x.getGruppe() == Gruppe.AKTORID)
                .map(PDLIdent::getIdent).map(AktorId::of)
                .toList();
        List<Fnr> alleFolkeregisterIdenterForBruker = alleIdenterForBruker
                .stream()
                .filter(x -> x.getGruppe() == Gruppe.FOLKEREGISTERIDENT)
                .map(PDLIdent::getIdent)
                .map(Fnr::new)
                .toList();

        alleAktoerIdForBruker.forEach((aktorId) -> {
            oppfolgingRepositoryV2.slettOppfolgingData(aktorId);
            registreringService.slettRegistering(aktorId);
            endringIRegistreringService.slettEndringIRegistering(aktorId);
            arbeidslisteService.slettArbeidsliste(aktorId);
            sisteEndringService.slettSisteEndringer(aktorId);
            cvRepositoryV2.resetHarDeltCV(aktorId);
            siste14aVedtakService.slettSiste14aVedtak(aktorId.get());
            pdlService.slettPdlData(aktorId);
            ensligeForsorgereService.slettEnsligeForsorgereData(aktorId);
            profileringService.slettProfileringData(aktorId);
            // TODO: Delete from rest of the tables which are missing
        });

        alleFolkeregisterIdenterForBruker.forEach(folkeregisterIdent -> {
            huskelappService.slettAlleHuskelapperPaaBruker(folkeregisterIdent);
            fargekategoriService.fjernFargekategoriForBruker(folkeregisterIdent);
        });

        opensearchIndexerV2.slettDokumenter(alleAktoerIdForBruker);
        secureLog.info(
                "Bruker: {} har avsluttet oppfølging og er slettet. Slettet også data for historiske identer {} for samme bruker.",
                aktoerId,
                String.join(", ", alleAktoerIdForBruker.stream().map(AktorId::get).toList())
        );
    }
}
