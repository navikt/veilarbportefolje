package no.nav.fo.mock;

import io.vavr.control.Try;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;
import no.nav.fo.service.AktoerService;

public class AktoerServiceMock implements AktoerService {

    public static final String FNR = "99999999991";
    public static final String FNR_2 = "99999999992";
    public static final String FNR_FAIL = "99999999990";
    public static final String FNR_UNAUTHORIZED = "99999999900";

    public static final String PERSON_ID = "9991";
    public static final String PERSON_ID_2 = "9992";
    public static final String PERSON_ID_UNAUTHORIZED = "9900";

    public static final String AKTOER_ID = "9999999999991";
    public static final String AKTOER_ID_2 = "9999999999992";
    public static final String AKTOER_ID_FAIL = "9999999999990";
    public static final String AKTOER_ID_UNAUTHORIZED = "9999999999900";


    @Override
    public Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerid) {
        return Try.success(PERSON_ID).map(PersonId::new);
    }

    @Override
    public Try<AktoerId> hentAktoeridFraPersonid(String personid) {
        return Try.success(AKTOER_ID).map(AktoerId::new);
    }

    @Override
    public Try<AktoerId> hentAktoeridFraFnr(Fnr fnr) {
        if (new Fnr(FNR_FAIL).equals(fnr)) {
            return Try.success(AKTOER_ID_FAIL).map(AktoerId::new);
        } else if (new Fnr(FNR_UNAUTHORIZED).equals(fnr)) {
            return Try.success(AKTOER_ID_UNAUTHORIZED).map(AktoerId::new);
        }
        return Try.success(AKTOER_ID).map(AktoerId::new);
    }

    @Override
    public Try<Fnr> hentFnrFraAktoerid(AktoerId aktoerid) {
        return getTestFnr(aktoerid);
    }

    private static Try<Fnr> getTestFnr(AktoerId aktoerId) {
        Try<String> result = Try.success(FNR);
        if (new AktoerId(AKTOER_ID_FAIL).equals(aktoerId)) {
            result = Try.failure(new RuntimeException());
        } else if (new AktoerId(AKTOER_ID_UNAUTHORIZED).equals(aktoerId)) {
            result = Try.success(FNR_FAIL);
        }
        return result.map(Fnr::new);
    }
}
