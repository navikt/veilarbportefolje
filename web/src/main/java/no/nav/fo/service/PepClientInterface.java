package no.nav.fo.service;

public interface PepClientInterface {
    boolean isSubjectAuthorizedToSeeKode7(String token);
    boolean isSubjectAuthorizedToSeeKode6(String token);
    boolean isSubjectAuthorizedToSeeEgenAnsatt(String token);
}
