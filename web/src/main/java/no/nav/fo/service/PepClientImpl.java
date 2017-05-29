package no.nav.fo.service;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;


public class PepClientImpl implements PepClient {

    private Logger logger = LoggerFactory.getLogger(PepClientImpl.class);

    private Pep pep;

    @Inject
    public PepClientImpl(Pep pep) {
        this.pep = pep;
    }

    @Cacheable("brukertilgangCache")
    public boolean isSubjectAuthorizedToSeeKode7(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeKode7(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Cacheable("brukertilgangCache")
    public boolean isSubjectAuthorizedToSeeKode6(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeKode6(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Cacheable("brukertilgangCache")
    public boolean isSubjectAuthorizedToSeeEgenAnsatt(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeEgenAnsatt(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Cacheable("brukertilgangCache")
    public boolean isSubjectMemberOfModiaOppfolging(String ident, String token) {
        BiasedDecisionResponse callAllowed;
        try {
            Timer timer = MetricsFactory.createTimer("isSubjectMemberOfModiaOppfolging");
            timer.start();
            callAllowed  = pep.isSubjectMemberOfModiaOppfolging(token, "veilarb");
            timer.stop();
            timer.report();
        } catch (PepException e) {
            throw new InternalServerErrorException("Something went wrong when wrong in PEP", e);
        }
        if (callAllowed.getBiasedDecision().equals(Decision.Deny)) {
            logger.info("User "+ ident +" is not in group MODIA-OPPFOLGING");
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }
}
