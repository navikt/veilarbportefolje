package no.nav.pto.veilarbportefolje.admin;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
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
            Try<List<String>> allAktoerIds = brukerRepository.getAllAktoerIds();
            allAktoerIds.get().stream().forEach(aktoerId -> {
                Fnr fnr = aktorregisterClient.hentFnr(AktorId.of(aktoerId));
                Try<PersonId> personIdOptional = brukerRepository.retrievePersonidFromFnr(fnr);
                PersonId personId = personIdOptional.getOrNull();
                if (personId != null){
                    brukerRepository.insertAktoeridToPersonidMapping(AktorId.of(aktoerId), personId);
                }else{
                    log.warn("Cant get personId for aktoerid during cleanup " + aktoerId);
                }
            });
            log.info("Cleanup aktoerId to personId er ferdig!");
            return true;
        }catch (Exception e){
            log.warn("Error during cleanup "+ e, e);
            return false;
        }
    }
}

