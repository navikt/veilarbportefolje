package no.nav.fo.mock;

import no.nav.fo.service.AktoerService;

import java.util.Optional;

public class AktoerServiceMock implements AktoerService {

    private static final String FAILING_AKTOERID = "75843973123";

    private static final String FNR = "99999999999";
    private static final String PERSON_ID = "9999";
    private static final String AKTOER_ID = "9999999999999";

    public static String getFailingAktoerid() {
        return FAILING_AKTOERID;
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
        return Optional.of(AKTOER_ID);
    }

    @Override
    public Optional<String> hentFnrFraAktoerid(String aktoerid) {
        return getTestFnr(aktoerid);
    }

    public static Optional<String> getTestFnr(String aktoerId) {
        Optional<String> result = Optional.of(FNR);
        if (FAILING_AKTOERID.equals(aktoerId)) {
            result = Optional.empty();
        }
        return result;
    }
}
