package no.nav.fo.service;

import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;


public class PepClient implements PepClientInterface {

    @Inject
    private Pep pep;

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
}
