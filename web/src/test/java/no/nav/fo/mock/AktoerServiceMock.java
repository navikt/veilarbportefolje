package no.nav.fo.mock;

import no.nav.fo.service.AktoerService;

import java.util.Optional;

public class AktoerServiceMock implements AktoerService {

    private static final String FAILING_FNR = "75843973123";

    public static String getFailingFnr() {
        return FAILING_FNR;
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

    private Optional<String> returnTestData(String string) {
        Optional<String> result = Optional.of(createTestId(string));
        if (FAILING_FNR.equals(string)) {
            result = Optional.empty();
        }
        return result;
    }

    public static String createTestId(String string) {
        return "_" + string;
    }
}
