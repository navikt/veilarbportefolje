package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@RequiredArgsConstructor
public class FargekategoriService {

    private final FargekategoriRepository fargekategoriRepository;
    private final PdlIdentRepository pdlIdentRepository;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final BrukerServiceV2 brukerServiceV2;

    public Optional<FargekategoriEntity> hentFargekategoriForBruker(FargekategoriController.HentFargekategoriRequest request) {
        return fargekategoriRepository.hentFargekategoriForBruker(request.fnr());
    }

    public void batchoppdaterFargekategoriForBruker(FargekategoriVerdi fargekategoriVerdi, List<Fnr> fnr, VeilederId innloggetVeileder, EnhetId enhetId) {
        if (fargekategoriVerdi == FargekategoriVerdi.INGEN_KATEGORI) {
            fargekategoriRepository.batchdeleteFargekategori(fnr);

            fnr.forEach(this::slettIOpensearch);

        } else {
            fargekategoriRepository.batchupsertFargekategori(fargekategoriVerdi, fnr, innloggetVeileder, enhetId);

            fnr.forEach(f -> oppdaterIOpensearch(f, fargekategoriVerdi.name(), enhetId.get()));
        }
    }

    private void slettIOpensearch(Fnr fnr) {
        AktorId aktorId = Optional.ofNullable(pdlIdentRepository.hentAktorIdForAktivBruker(fnr)).orElseThrow(RuntimeException::new);
        opensearchIndexerV2.slettFargekategori(aktorId);
    }

    private void oppdaterIOpensearch(Fnr fnr, String fargekategori, String enhetId) {
        AktorId aktorId = Optional.ofNullable(pdlIdentRepository.hentAktorIdForAktivBruker(fnr)).orElseThrow(RuntimeException::new);
        opensearchIndexerV2.updateFargekategori(aktorId, fargekategori, enhetId);
    }

    public void slettFargekategoriPaaBruker(AktorId aktorId, Optional<Fnr> maybeFnr) {
        try {
            secureLog.info("Sletter fargekategori på bruker med aktoerid: " + aktorId);
            if (maybeFnr.isPresent()) {
                fargekategoriRepository.deleteFargekategori(maybeFnr.get());
                opensearchIndexerV2.slettFargekategori(aktorId);
            } else {
                secureLog.warn("Kunne ikke slette fargekategori for bruker med AktørID {}. Årsak fødselsnummer-parameter var tom.", aktorId.get());
            }
        } catch (Exception e) {
            secureLog.error("Kunne ikke slette fagekategori for aktoerId: " + aktorId.toString(), e);
            throw new RuntimeException("Kunne ikke slette fagekategori");
        }
    }

    public void oppdaterEnhetPaaFargekategori(Fnr fnr, EnhetId enhetId, VeilederId veilederId) {
        try {
            Optional<FargekategoriEntity> fargekategoriForBruker = fargekategoriRepository.hentFargekategoriForBruker(fnr);
            fargekategoriForBruker.ifPresent(fargekategoriEntity -> {
                OppdaterFargekategoriRequest fargekategoriMedNyEnhet = new OppdaterFargekategoriRequest(fnr, fargekategoriForBruker.get().fargekategoriVerdi());
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

        Optional<String> navkontorPaFargekategori = fargekategoriRepository.hentNavkontorPaFargekategori(maybeFnr.get());

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
}
