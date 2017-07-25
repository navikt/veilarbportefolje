package no.nav.fo.service;

import io.vavr.control.Try;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;

public interface AktoerService {
    Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerid);
    Try<AktoerId> hentAktoeridFraPersonid(String personid);
    Try<AktoerId> hentAktoeridFraFnr(Fnr fnr);
    Try<Fnr> hentFnrFraAktoerid(AktoerId aktoerid);
}
