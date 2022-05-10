package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
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

    public void hentOgLagrePdlData(AktorId aktorId){
        hentOgLagreIdenter(aktorId);
        hentOgLagreBrukerData(aktorId);
    }

    private void hentOgLagreIdenter(AktorId aktorId) {
        log.info("Oppdaterer ident mapping for aktor: {}", aktorId);

        List<PDLIdent> idents = pdlClient.hentIdenterFraPdl(aktorId);
        pdlIdentRepository.upsertIdenter(idents);
    }

    private void hentOgLagreBrukerData(AktorId aktorId) {
        log.info("Oppdaterer ident mapping for aktor: {}", aktorId);

        PDLPerson personData = pdlClient.hentBrukerDataFraPdl(aktorId);
        pdlPersonRepository.upsertPerson(personData);
    }

    @Transactional
    public void slettPdlData(AktorId aktorId) {
        String lokalIdent = pdlIdentRepository.hentPerson(aktorId.get());
        List<PDLIdent> identer = pdlIdentRepository.hentIdenter(lokalIdent);

        if (pdlIdentRepository.harIdentUnderOppfolging(identer)) {
            log.warn("""
                            Sletter ikke identer tilknyttet aktorId: {}.
                            Da en eller flere relaterte identer på person: {} er under oppfolging.
                            """,
                    aktorId, lokalIdent);
            return;
        }
        log.info("Sletter identer og brukerdata for aktor: {}", aktorId);
        pdlPersonRepository.slettLagretBrukerData(identer);
        pdlIdentRepository.slettLagretePerson(lokalIdent);
    }
}
