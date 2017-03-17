package no.nav.fo.service;

import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;


public class PepClient {

    @Inject
    private Pep pep;

    @Cacheable("brukertilgangCache")
    public boolean isServiceCallAllowed(String fnr, String ident) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isServiceCallAllowedWithIdent(ident, "veilarb", fnr);
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }
}
