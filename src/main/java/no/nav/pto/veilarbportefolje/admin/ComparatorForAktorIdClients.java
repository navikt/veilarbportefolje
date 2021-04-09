package no.nav.pto.veilarbportefolje.admin;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class ComparatorForAktorIdClients {
    private final AktorOppslagClient aktorOppslagClient;
    private final AktorregisterClient aktorregisterClient;
    private final BrukerRepository brukerRepository;

    public Boolean erAktorIdsErSamme(String fnr){
        log.info("Testing difference between actorIds....");
        try{
            AktorId aktorId1 = aktorOppslagClient.hentAktorId(Fnr.of(fnr));
            AktorId aktorId2 = aktorregisterClient.hentAktorId(Fnr.of(fnr));
            if (!aktorId1.equals(aktorId2)){
                return false;
            }
            return true;
        }
        catch (Exception e){
            log.warn("Error erAktorIdsErSamme: "+e, e);
            return null;
        }
    }
}
