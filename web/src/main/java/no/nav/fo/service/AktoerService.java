package no.nav.fo.service;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;

import java.util.Optional;

public interface AktoerService {
    Optional<PersonId> hentPersonidFraAktoerid(AktoerId aktoerid);
    Optional<AktoerId> hentAktoeridFraPersonid(String personid);
    Optional<AktoerId> hentAktoeridFraFnr(Fnr fnr);
    Optional<Fnr> hentFnrFraAktoerid(AktoerId aktoerid);
}
