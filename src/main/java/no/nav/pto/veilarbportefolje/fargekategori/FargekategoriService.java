package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@RequiredArgsConstructor
public class FargekategoriService {

    private final FargekategoriRepository fargekategoriRepository;
    private final PdlIdentRepository pdlIdentRepository;
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final BrukerServiceV2 brukerServiceV2;

    public Optional<FargekategoriEntity> hentFargekategoriForBruker(FargekategoriController.HentFargekategoriRequest request) {
        return hentPrioritertFargekategori(request.fnr(), hentAlleFnrIdenterForBruker(request.fnr()));
    }

    @Transactional
    public void batchoppdaterFargekategoriForBruker(FargekategoriVerdi fargekategoriVerdi, List<Fnr> fnr, VeilederId innloggetVeileder, EnhetId enhetId) {
        List<Fnr> alleFnrIdenter = fnr.stream()
                .flatMap(f -> hentAlleFnrIdenterForBruker(f).stream())
                .distinct()
                .toList();

        if (fargekategoriVerdi == FargekategoriVerdi.INGEN_KATEGORI) {
            fargekategoriRepository.deleteFargekategori(alleFnrIdenter);

            fnr.forEach(this::slettIOpensearch);

        } else {
            fargekategoriRepository.deleteFargekategori(alleFnrIdenter.stream().filter(ident -> !fnr.contains(ident)).toList());
            fargekategoriRepository.batchupsertFargekategori(fargekategoriVerdi, fnr, innloggetVeileder, enhetId);
            fnr.forEach(f -> oppdaterIOpensearch(f, fargekategoriVerdi.name(), enhetId.get()));
        }
    }

    private void slettIOpensearch(Fnr fnr) {
        Optional<AktorId> aktorId = brukerServiceV2.hentAktorId(fnr);
        aktorId.ifPresent(opensearchIndexerPaDatafelt::slettFargekategori);
    }

    private void oppdaterIOpensearch(Fnr fnr, String fargekategori, String enhetId) {
        Optional<AktorId> aktorId = brukerServiceV2.hentAktorId(fnr);
        aktorId.ifPresent(a -> opensearchIndexerPaDatafelt.updateFargekategori(a, fargekategori, enhetId));
    }

    public void slettFargekategoriPaaBruker(AktorId aktorId, Optional<Fnr> maybeFnr) {
        try {
            secureLog.info("Sletter fargekategori på bruker med aktoerid: " + aktorId);
            if (maybeFnr.isPresent()) {
                fargekategoriRepository.deleteFargekategori(hentAlleFnrIdenterForBruker(maybeFnr.get()));
                opensearchIndexerPaDatafelt.slettFargekategori(aktorId);
            } else {
                secureLog.warn("Kunne ikke slette fargekategori for bruker med AktørID {}. Årsak fødselsnummer-parameter var tom.", aktorId.get());
            }
        } catch (Exception e) {
            secureLog.error("Kunne ikke slette fagekategori for aktoerId: " + aktorId.toString(), e);
            throw new RuntimeException("Kunne ikke slette fagekategori");
        }
    }

    @Transactional
    public void oppdaterEnhetPaaFargekategori(Fnr fnr, EnhetId enhetId, VeilederId veilederId) {
        try {
            List<Fnr> fnrIdenter = hentAlleFnrIdenterForBruker(fnr);
            Optional<FargekategoriEntity> fargekategoriForBruker = hentPrioritertFargekategori(fnr, fnrIdenter);
            fargekategoriForBruker.ifPresent(fargekategoriEntity -> {
                OppdaterFargekategoriRequest fargekategoriMedNyEnhet = new OppdaterFargekategoriRequest(fnr, fargekategoriEntity.fargekategoriVerdi());
                fargekategoriRepository.deleteFargekategori(fnrIdenter.stream().filter(ident -> !ident.equals(fnr)).toList());
                fargekategoriRepository.upsertFargekateori(fargekategoriMedNyEnhet, veilederId, enhetId);
                oppdaterIOpensearch(fargekategoriMedNyEnhet.fnr(), fargekategoriMedNyEnhet.fargekategoriVerdi().name(), enhetId.get());
            });
        } catch (Exception e) {
            secureLog.error("Kunne ikke oppdatere enhet på fargekategori for bruker: " + fnr, e);
            throw new RuntimeException("Kunne ikke oppdatere enhet på fargekategori");
        }
    }

    public boolean brukerHarFargekategoriPaForrigeNavkontor(AktorId aktoerId, Optional<Fnr> maybeFnr) {
        if (maybeFnr.isEmpty()) {
            return false;
        }

        Optional<String> navkontorPaFargekategori = hentPrioritertFargekategori(maybeFnr.get(), hentAlleFnrIdenterForBruker(maybeFnr.get()))
                .map(FargekategoriEntity::enhetId)
                .map(EnhetId::get);

        if (navkontorPaFargekategori.isEmpty()) {
            secureLog.info("Bruker {} har ikke NAV-kontor på fargekategori", aktoerId.toString());
            return false;
        }

        final Optional<String> navKontorForBruker = brukerServiceV2.hentNavKontor(aktoerId).map(NavKontor::getValue);
        if (navKontorForBruker.isEmpty()) {
            secureLog.error("Kunne ikke hente NAV-kontor for bruker {}", aktoerId.toString());
            return false;
        }

        boolean navkontorForBrukerUlikNavkontorPaFargekategori = !navKontorForBruker.orElseThrow().equals(navkontorPaFargekategori.orElseThrow());

        if (navkontorForBrukerUlikNavkontorPaFargekategori) {
            secureLog.info("Bruker {} er på kontor {} mens fargekategori er lagret på et annet kontor {}", aktoerId.toString(), navKontorForBruker.get(), navkontorPaFargekategori.get());
        } else {
            secureLog.info("Bruker {} er på kontor {} og fargekategori er lagret på samme kontor {}", aktoerId.toString(), navKontorForBruker.get(), navkontorPaFargekategori.get());
        }

        return navkontorForBrukerUlikNavkontorPaFargekategori;
    }

    private Optional<FargekategoriEntity> hentPrioritertFargekategori(Fnr brukerFnr, List<Fnr> fnrIdenter) {
        List<FargekategoriEntity> fargekategorier = fargekategoriRepository.hentFargekategorierForIdenter(fnrIdenter);
        if (fargekategorier.size() <= 1) {
            return fargekategorier.stream().findFirst();
        }
        return fargekategorier.stream()
                .filter(fargekategori -> fargekategori.fnr().equals(brukerFnr))
                .findFirst()
                .or(() -> fargekategorier.stream().findFirst());
    }

    private List<Fnr> hentAlleFnrIdenterForBruker(Fnr fnr) {
        Set<String> identer = new LinkedHashSet<>(pdlIdentRepository.hentFnrIdenterForBruker(fnr.get()).identer());
        identer.add(fnr.get());
        return identer.stream().map(Fnr::of).toList();
    }
}
