package no.nav.fo.veilarbportefolje.service;

import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import static no.nav.fo.veilarbportefolje.provider.rest.ArbeidslisteRessursTest.UNAUTHORIZED_NAV_KONTOR;

public class PepClientMock implements PepClient {

    @Override
    public boolean isSubjectAuthorizedToSeeKode7(String token) {
        return true;
    }

    @Override
    public boolean isSubjectAuthorizedToSeeKode6(String token) {
        return true;
    }

    @Override
    public boolean isSubjectAuthorizedToSeeEgenAnsatt(String token) {
        return true;
    }

    @Override
    public boolean isSubjectMemberOfModiaOppfolging(String ident, String token) {
        return true;
    }

    @Override
    public boolean tilgangTilBruker(String fnr) {
        return true;
    }

    @Override
    public boolean tilgangTilEnhet(String ident, String enhet) {
        return !enhet.equals(UNAUTHORIZED_NAV_KONTOR);
    }

    @Override
    public void ping() throws PepException {

    }
}
