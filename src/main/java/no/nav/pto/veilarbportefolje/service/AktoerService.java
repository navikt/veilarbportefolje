package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AktoerService {
    Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerid);
    Try<AktoerId> hentAktoeridFraFnr(Fnr fnr);
    Map<Fnr, Optional<PersonId>> hentPersonidsForFnrs(List<Fnr> fnrs);
}
