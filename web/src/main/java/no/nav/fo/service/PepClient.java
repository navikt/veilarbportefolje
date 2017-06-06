package no.nav.fo.service;

import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public interface PepClient {
    boolean isSubjectAuthorizedToSeeKode7(String token);
    boolean isSubjectAuthorizedToSeeKode6(String token);
    boolean isSubjectAuthorizedToSeeEgenAnsatt(String token);
    boolean isSubjectMemberOfModiaOppfolging(String ident, String token);
    boolean tilgangTilBruker(String token, String fnr);
    void ping() throws PepException;
}
