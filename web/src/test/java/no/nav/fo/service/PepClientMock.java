package no.nav.fo.service;

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
}
