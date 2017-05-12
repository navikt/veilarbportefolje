package no.nav.fo.service;

public interface PepClient {
    boolean isSubjectAuthorizedToSeeKode7(String token);
    boolean isSubjectAuthorizedToSeeKode6(String token);
    boolean isSubjectAuthorizedToSeeEgenAnsatt(String token);
    boolean isSubjectMemberOfModiaOppfolging(String ident, String token);
}
