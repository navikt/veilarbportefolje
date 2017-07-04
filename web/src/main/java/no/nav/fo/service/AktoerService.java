package no.nav.fo.service;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;

import java.util.Optional;

public interface AktoerService {
    public Optional<PersonId> hentPersonidFraAktoerid(AktoerId aktoerid);
    public Optional<AktoerId> hentAktoeridFraPersonid(String personid);
    public Optional<AktoerId> hentAktoeridFraFnr(Fnr fnr);
    public Optional<Fnr> hentFnrFraAktoerid(AktoerId aktoerid);
}
