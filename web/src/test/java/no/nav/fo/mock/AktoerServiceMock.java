package no.nav.fo.mock;

import no.nav.fo.service.AktoerService;

import java.util.Optional;

public class AktoerServiceMock implements AktoerService {

    public static final String FNR = "99999999991";
    public static final String FNR_FAIL = "99999999992";

    public static final String PERSON_ID = "9991";

    public static final String AKTOER_ID = "9999999999991";
    public static final String AKTOER_ID_FAIL = "9999999999992";

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
        if (FNR_FAIL.equals(fnr)) {
            return Optional.of(AKTOER_ID_FAIL);
        }
        return Optional.of(AKTOER_ID);
    }

    @Override
    public Optional<String> hentFnrFraAktoerid(String aktoerid) {
        return getTestFnr(aktoerid);
    }

    private static Optional<String> getTestFnr(String aktoerId) {
        Optional<String> result = Optional.of(FNR);
        if (AKTOER_ID_FAIL.equals(aktoerId)) {
            result = Optional.empty();
        }
        return result;
    }
}
