package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.AktoerId;

public class NavKontorService {

    public Try<String> hentEnhetForBruker(AktoerId aktoerId) {
        return Try.failure(new IllegalStateException());
    }
}
