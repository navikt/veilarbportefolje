package no.nav.pto.veilarbportefolje.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.util.Result;

@Slf4j
public class NavKontorService {

    private final BrukerRepository brukerRepository;

    public NavKontorService(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    public Result<String> hentEnhetForBruker(Fnr fnr) {
        return brukerRepository.hentEnhetForBruker(fnr);
    }
}
