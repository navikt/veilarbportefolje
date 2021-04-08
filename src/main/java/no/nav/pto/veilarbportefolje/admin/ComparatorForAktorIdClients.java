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

    public void testAktorIds(Integer totalFnrsToTest){
        AtomicInteger counter = new AtomicInteger();
        Try<List<Fnr>> listFnrs = brukerRepository.hentRandomFnrs(totalFnrsToTest);

        log.info("Testing difference between actorIds....");
        listFnrs.get().stream().forEach(fnr -> {
            AktorId aktorId1 = aktorOppslagClient.hentAktorId(fnr);
            AktorId aktorId2 = aktorregisterClient.hentAktorId(fnr);

            if (!aktorId1.equals(aktorId2)){
                counter.getAndIncrement();
            }
        });

        log.info("Different actorIds: {} from: {} tested", counter, totalFnrsToTest);
    }
}
