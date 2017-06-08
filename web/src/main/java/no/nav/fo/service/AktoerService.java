package no.nav.fo.service;

import java.util.Optional;

public interface AktoerService {
    public Optional<String> hentPersonidFraAktoerid(String aktoerid);
    public Optional<String> hentAktoeridFraPersonid(String personid);
    public Optional<String> hentAktoeridFraFnr(String fnr);
    public Optional<String> hentFnrFraAktoerid(String aktoerid);
}
