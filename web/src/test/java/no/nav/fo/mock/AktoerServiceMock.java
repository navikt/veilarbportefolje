package no.nav.fo.mock;

import no.nav.fo.service.AktoerService;

import java.util.Optional;

public class AktoerServiceMock implements AktoerService {

    private static final String FAILING_ID = "FAILING_ID";
    private static final String TEST_ID = "TEST_ID";

    public static String getFailingId() {
        return FAILING_ID;
    }

    @Override
    public Optional<String> hentPersonidFraAktoerid(String aktoerid) {
        return returnTestData(aktoerid);
    }


    @Override
    public Optional<String> hentAktoeridFraPersonid(String personid) {
        return returnTestData(personid);
    }

    @Override
    public Optional<String> hentAktoeridFraFnr(String fnr) {
        return returnTestData(fnr);
    }

    @Override
    public Optional<String> hentFnrFraAktoerid(String aktoerid) {
        return returnTestData(aktoerid);
    }

    private Optional<String> returnTestData(String aktoerid) {
        Optional<String> result = Optional.of(TEST_ID);
        if (FAILING_ID.equals(aktoerid)) {
            result = Optional.empty();
        }
        return result;
    }
}
