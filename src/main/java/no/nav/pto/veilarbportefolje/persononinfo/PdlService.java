package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Barn;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.RelasjonsBosted.SAMME_BOSTED;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlService {
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;

    private final BarnUnder18AarRepository barnUnder18AarRepository;
    private final PdlPortefoljeClient pdlClient;

    public void hentOgLagrePdlData(AktorId aktorId) {
        List<PDLIdent> identer = hentOgLagreIdenter(aktorId);
        Fnr fnr = hentAktivFnr(identer);
        secureLog.info("Oppdaterer pdl brukerdata for aktor: {}", aktorId);
        hentOgLagreBrukerData(fnr);
    }

    public void hentOgLagreBrukerData(Fnr fnrPerson) {
        PDLPerson personData = pdlClient.hentBrukerDataFraPdl(fnrPerson);
        pdlPersonRepository.upsertPerson(fnrPerson, personData);

        List<PDLPersonBarn> brukerBarn = new ArrayList<>();

        if (personData.getBarn() != null) {
            personData.getBarn().forEach(barnFnr -> {
                PDLPersonBarn barn = pdlClient.hentBrukerBarnDataFraPdl(fnrPerson);
                barnUnder18AarRepository.upsert2(barnFnr, fnrPerson, barn.getFodselsdato(), barn.getDiskresjonskode());
            });
        }
    }

    private List<PDLIdent> hentOgLagreIdenter(AktorId aktorId) {
        secureLog.info("Oppdaterer ident mapping for aktor: {}", aktorId);

        List<PDLIdent> identer = pdlClient.hentIdenterFraPdl(aktorId);
        pdlIdentRepository.upsertIdenter(identer);
        return identer;
    }

    @Transactional
    public void slettPdlData(AktorId aktorId) {
        String lokalIdent = pdlIdentRepository.hentPerson(aktorId.get());
        List<PDLIdent> identer = pdlIdentRepository.hentIdenter(lokalIdent);
        List<AktorId> aktorIds = identer.stream()
                .filter(ident -> PDLIdent.Gruppe.AKTORID.equals(ident.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new).toList();
        List<Fnr> fnrs = identer.stream()
                .filter(ident -> PDLIdent.Gruppe.FOLKEREGISTERIDENT.equals(ident.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(Fnr::new).toList();

        if (pdlIdentRepository.harAktorIdUnderOppfolging(aktorIds)) {
            secureLog.warn("""
                            Sletter ikke identer tilknyttet aktorId: {}.
                            Da en eller flere relaterte identer på person: {} er under oppfolging.
                            """,
                    aktorId, lokalIdent);
            return;
        }
        secureLog.info("Sletter identer og brukerdata for aktor: {}", aktorId);
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

    public Map<Fnr, List<Statsborgerskap>> hentStatsborgerskap(List<Fnr> fnrs) {
        return pdlPersonRepository.hentGyldigeStatsborgerskapData(fnrs);
    }

}
