package no.nav.fo.service;

public class PepClientMock implements PepClientInterface {

    @Override
    public boolean isSubjectAuthorizedToSeeKode7(String ident) {
        return true;
    }

    @Override
    public boolean isSubjectAuthorizedToSeeKode6(String ident) {
        return true;
    }

    @Override
    public boolean isSubjectAuthorizedToSeeEgenAnsatt(String ident) {
        return true;
    }
}
