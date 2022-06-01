package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlService {
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final PdlPortefoljeClient pdlClient;

    public void hentOgLagrePdlData(AktorId aktorId) {
        List<PDLIdent> identer = hentOgLagreIdenter(aktorId);
        Fnr fnr = hentAktivFnr(identer);
        log.info("Oppdaterer pdl brukerdata for aktor: {}", aktorId);
        hentOgLagreBrukerData(fnr);
    }

    public void hentOgLagreBrukerData(Fnr fnr) {
        PDLPerson personData = pdlClient.hentBrukerDataFraPdl(fnr);
        pdlPersonRepository.upsertPerson(fnr, personData);
    }

    private List<PDLIdent> hentOgLagreIdenter(AktorId aktorId) {
        log.info("Oppdaterer ident mapping for aktor: {}", aktorId);

        List<PDLIdent> identer = pdlClient.hentIdenterFraPdl(aktorId);
        pdlIdentRepository.upsertIdenter(identer);
        return identer;
    }

    @Transactional
    public void slettPdlData(AktorId aktorId) {
        String lokalIdent = pdlIdentRepository.hentPerson(aktorId.get());
        List<PDLIdent> identer = pdlIdentRepository.hentIdenter(lokalIdent);
        List<AktorId> aktorIds = identer.stream()
                .filter(x -> PDLIdent.Gruppe.AKTORID.equals(x.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new).toList();
        List<Fnr> fnrs = identer.stream()
                .filter(x -> PDLIdent.Gruppe.FOLKEREGISTERIDENT.equals(x.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(Fnr::new).toList();

        if (pdlIdentRepository.harAktorIdUnderOppfolging(aktorIds)) {
            log.warn("""
                            Sletter ikke identer tilknyttet aktorId: {}.
                            Da en eller flere relaterte identer på person: {} er under oppfolging.
                            """,
                    aktorId, lokalIdent);
            return;
        }
        log.info("Sletter identer og brukerdata for aktor: {}", aktorId);
        pdlPersonRepository.slettLagretBrukerData(fnrs);
        pdlIdentRepository.slettLagretePerson(lokalIdent);
    }

    public static AktorId hentAktivAktor(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.AKTORID.equals(pdlIdent.getGruppe()))
                .filter(pdlIdent -> !pdlIdent.isHistorisk())
                .map(PDLIdent::getIdent)
                .map(AktorId::new)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Ingen aktiv aktør på bruker"));
    }

    public static Fnr hentAktivFnr(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.FOLKEREGISTERIDENT.equals(pdlIdent.getGruppe()))
                .filter(pdlIdent -> !pdlIdent.isHistorisk())
                .map(PDLIdent::getIdent)
                .map(Fnr::new)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Ingen aktiv fnr på bruker"));
    }
}
