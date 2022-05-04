package no.nav.pto.veilarbportefolje.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerServiceV2 {
    private final PdlRepository pdlRepository;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    public Optional<AktorId> hentAktorId(Fnr fnr) {
        return Optional.ofNullable(pdlRepository.hentAktorId(fnr));
    }

    public Optional<NavKontor> hentNavKontor(AktorId aktoerId) {
        Fnr fnr = pdlRepository.hentFnr(aktoerId);
        return hentNavKontor(fnr);
    }

    public Optional<NavKontor> hentNavKontor(Fnr fnr) {
        return oppfolgingsbrukerRepositoryV3.hentNavKontor(fnr);
    }

    public Optional<VeilederId> hentVeilederForBruker(AktorId aktoerId) {
        return oppfolgingRepositoryV2.hentVeilederForBruker(aktoerId);
    }
}
