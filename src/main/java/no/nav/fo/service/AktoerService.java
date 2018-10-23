package no.nav.fo.service;

import io.vavr.control.Try;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AktoerService {
    Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerid);
    Try<AktoerId> hentAktoeridFraFnr(Fnr fnr);
    Map<Fnr, Optional<PersonId>> hentPersonidsForFnrs(List<Fnr> fnrs);
}
