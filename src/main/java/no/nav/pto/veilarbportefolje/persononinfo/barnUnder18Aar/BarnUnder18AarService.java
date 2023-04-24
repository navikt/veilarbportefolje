package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarnUnder18AarService {

    private final BarnUnder18AarRepository barnUnder18AarRepository;

    List<BarnUnder18AarData> data = new ArrayList<>();
    //BarnUnder18AarData data2 = (1L, true, "");


    public Map<Fnr, List<BarnUnder18AarData>> hentBarnUnder18AarAlle(List<Fnr> fnrs) {
        Map<Fnr, List<BarnUnder18AarData>> result = new HashMap<>();
        Map<Fnr, List<BarnUnder18AarData>> result2 = new HashMap<>();

        fnrs.forEach( fnr ->
                        result.put(fnr,barnUnder18AarRepository.hentBarnUnder18Aar(fnr.toString()))
        );

        //data.add(new BarnUnder18AarData(1L, true, ""));
        return result;
}
}
