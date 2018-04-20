package no.nav.fo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;

@Slf4j
public class PepClientImpl implements PepClient {

    private Pep pep;

    @Inject
    public PepClientImpl(Pep pep) {
        this.pep = pep;
    }

    public boolean isSubjectAuthorizedToSeeKode7(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeKode7(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return Permit.equals(callAllowed.getBiasedDecision());
    }

    public boolean isSubjectAuthorizedToSeeKode6(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeKode6(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return Permit.equals(callAllowed.getBiasedDecision());
    }

    public boolean isSubjectAuthorizedToSeeEgenAnsatt(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeEgenAnsatt(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return Permit.equals(callAllowed.getBiasedDecision());
    }

    public boolean isSubjectMemberOfModiaOppfolging(String ident, String token) {
        BiasedDecisionResponse callAllowed;
        try {
            Timer timer = MetricsFactory.createTimer("isSubjectMemberOfModiaOppfolging");
            timer.start();
            callAllowed = pep.isSubjectMemberOfModiaOppfolging(token, "veilarb");
            timer.stop();
            timer.report();
        } catch (PepException e) {
            throw new InternalServerErrorException("Something went wrong when wrong in PEP", e);
        }
        if (callAllowed.getBiasedDecision().equals(Decision.Deny)) {
            log.info("User " + ident + " is not in group MODIA-OPPFOLGING");
        }
        return Permit.equals(callAllowed.getBiasedDecision());
    }

    public boolean tilgangTilBruker(String fnr) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.harInnloggetBrukerTilgangTilPerson(fnr, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return Permit.equals(callAllowed.getBiasedDecision());
    }

    public boolean tilgangTilEnhet(String ident, String enhet) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.harTilgangTilEnhet(enhet, "srvveilarbportefolje", "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("Something went wrong in PEP", e);
        }
        return Permit.equals(callAllowed.getBiasedDecision());
    }

    @Override
    public void ping() throws PepException {
        pep.ping();
    }
}
