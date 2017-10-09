package no.nav.fo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.config.CacheConfig;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

@Slf4j
public class PepClientImpl implements PepClient {

    private Pep pep;

    @Inject
    public PepClientImpl(Pep pep) {
        this.pep = pep;
    }

    @Cacheable(CacheConfig.kode7Cache)
    public boolean isSubjectAuthorizedToSeeKode7(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeKode7(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Cacheable(CacheConfig.kode6Cache)
    public boolean isSubjectAuthorizedToSeeKode6(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeKode6(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Cacheable(CacheConfig.egenAnsattCache)
    public boolean isSubjectAuthorizedToSeeEgenAnsatt(String token) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.isSubjectAuthorizedToSeeEgenAnsatt(token, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Cacheable(CacheConfig.modiaOppfolgingCache)
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
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    // NB! Etter refactorering er ikke parameteren `token` lenger i bruk,
    // men lar den stå siden den er en del av cachenøkkelen.
    @Cacheable(CacheConfig.brukerTilgangCache)
    public boolean tilgangTilBruker(String token, String fnr) {
        BiasedDecisionResponse callAllowed;
        try {
            callAllowed = pep.harInnloggetBrukerTilgangTilPerson(fnr, "veilarb");
        } catch (PepException e) {
            throw new InternalServerErrorException("something went wrong in PEP", e);
        }
        return callAllowed.getBiasedDecision().equals(Decision.Permit);
    }

    @Override
    public void ping() throws PepException {
        pep.ping();
    }
}
