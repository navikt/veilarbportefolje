package no.nav.fo.mock;

import no.nav.fo.service.AktoerService;

import java.util.Optional;

public class AktoerServiceMock implements AktoerService {

    public static final String FAILING_FNR = "90909090909";

    public static final String FNR = "99999999999";
    public static final String PERSON_ID = "9999";
    public static final String AKTOER_ID = "9999999999999";

    public static final String UNAUTHORIZED_FNR = "11111111111";

    public static String getFailingFnr() {
        return FAILING_FNR;
    }

    @Override
    public Optional<String> hentPersonidFraAktoerid(String aktoerid) {
        return Optional.of(PERSON_ID);
    }


    @Override
    public Optional<String> hentAktoeridFraPersonid(String personid) {
        return Optional.of(AKTOER_ID);
    }
    @Override

    public Optional<String> hentAktoeridFraFnr(String fnr) {
        if (UNAUTHORIZED_FNR.equals(fnr)) {
            return Optional.of(FAILING_FNR);
        }
        return Optional.of(AKTOER_ID);
    }

    @Override
    public Optional<String> hentFnrFraAktoerid(String aktoerid) {
        return getTestFnr(aktoerid);
    }

    public static Optional<String> getTestFnr(String aktoerId) {
        Optional<String> result = Optional.of(FNR);
        if (FAILING_FNR.equals(aktoerId)) {
            result = Optional.empty();
        }
        return result;
    }
}
