package no.nav.pto.veilarbportefolje.admin;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.OppfolgingBrukerDto;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import oracle.ucp.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupService {
    private final AktorregisterClient aktorregisterClient;
    private final BrukerRepository brukerRepository;

    public boolean runCleanup(){
        try{
            log.info("Starting cleanup aktoerId to personId ");
            brukerRepository.slettAlleAktorIdToPersonId();
            Try<List<OppfolgingBrukerDto>> allFnrsFromArena = brukerRepository.getAllFnrsFromArena();
            allFnrsFromArena.get().stream().forEach(oppfolgingBrukerDto -> {
                AktorId aktorId = aktorregisterClient.hentAktorId(Fnr.of(oppfolgingBrukerDto.getFnr()));
                brukerRepository.insertAktoeridToPersonidMapping(aktorId, PersonId.of(oppfolgingBrukerDto.getPersonId()));
            });
            log.info("Cleanup aktoerId to personId er ferdig!");
            return true;
        }catch (Exception e){
            log.warn("Error during cleanup "+ e, e);
            return false;
        }
    }
}

