package no.nav.fo.service;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;

import java.util.Optional;

public interface AktoerService {
    public Optional<String> hentPersonidFraAktoerid(AktoerId aktoerid);
    public Optional<AktoerId> hentAktoeridFraPersonid(String personid);
    public Optional<AktoerId> hentAktoeridFraFnr(Fnr fnr);
    public Optional<Fnr> hentFnrFraAktoerid(AktoerId aktoerid);
}
