package no.nav.pto.veilarbportefolje.abac;

import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public interface PepClient {
    boolean isSubjectAuthorizedToSeeKode7(String token);
    boolean isSubjectAuthorizedToSeeKode6(String token);
    boolean isSubjectAuthorizedToSeeEgenAnsatt(String token);
    boolean isSubjectMemberOfModiaOppfolging(String ident, String token);
    boolean tilgangTilBruker(String fnr);
    boolean tilgangTilEnhet(String ident, String enhet);
    void ping() throws PepException;
}
