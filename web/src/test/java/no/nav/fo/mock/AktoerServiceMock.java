package no.nav.fo.mock;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.service.AktoerService;

import java.util.Optional;

public class AktoerServiceMock implements AktoerService {

    public static final String FNR = "99999999991";
    public static final String FNR_FAIL = "99999999992";

    public static final String PERSON_ID = "9991";

    public static final String AKTOER_ID = "9999999999991";
    public static final String AKTOER_ID_FAIL = "9999999999992";

    @Override
    public Optional<String> hentPersonidFraAktoerid(AktoerId aktoerid) {
        return Optional.of(PERSON_ID);
    }


    @Override
    public Optional<AktoerId> hentAktoeridFraPersonid(String personid) {
        return Optional.of(AKTOER_ID).map(AktoerId::new);
    }

    @Override
    public Optional<AktoerId> hentAktoeridFraFnr(Fnr fnr) {
        if (new Fnr(FNR_FAIL).equals(fnr)) {
            return Optional.of(AKTOER_ID_FAIL).map(AktoerId::new);
        }
        return Optional.of(AKTOER_ID).map(AktoerId::new);
    }

    @Override
    public Optional<Fnr> hentFnrFraAktoerid(AktoerId aktoerid) {
        return getTestFnr(aktoerid);
    }

    private static Optional<Fnr> getTestFnr(AktoerId aktoerId) {
        Optional<String> result = Optional.of(FNR);
        if (new AktoerId(AKTOER_ID_FAIL).equals(aktoerId)) {
            result = Optional.empty();
        }
        return result.map(Fnr::new);
    }
}
