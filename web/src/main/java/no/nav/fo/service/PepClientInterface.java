package no.nav.fo.service;

public interface PepClientInterface {
    boolean isSubjectAuthorizedToSeeKode7(String ident);
    boolean isSubjectAuthorizedToSeeKode6(String ident);
    boolean isSubjectAuthorizedToSeeEgenAnsatt(String ident);
}
