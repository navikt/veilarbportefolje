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

    public Map<Fnr, List<BarnUnder18AarData>> hentBarnUnder18AarAlle(List<Fnr> fnrs) {
        Map<Fnr, List<BarnUnder18AarData>> result = new HashMap<>();

        fnrs.forEach( fnr ->
                        result.put(fnr,barnUnder18AarRepository.hentBarnUnder18Aar(fnr.toString()))
        );

        return result;
}

    public void lagreBarnOgForeldreansvar(Fnr foresattIdent, List<BarnUnder18Aar> barn){
        List<Fnr> lagredeBarn = barnUnder18AarRepository.hentForeldreansvarForPerson(foresattIdent);

        List<Fnr> barnFnrFraPdl = barn.stream().map(BarnUnder18Aar::getFnr).toList();

        lagredeBarn.forEach( barnFnr -> {
                    if (!barnFnrFraPdl.contains(barnFnr)){
                        barnUnder18AarRepository.slettForeldreansvar(foresattIdent, barnFnr);
                        if (!barnUnder18AarRepository.finnesBarnIForeldreansvar(barnFnr)){
                            barnUnder18AarRepository.slettBarnData(barnFnr);
                        }
                    }
                }
        );

        barn.forEach(barnUnder18Aar -> {
            barnUnder18AarRepository.lagreBarnData(barnUnder18Aar.getFnr(), barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());
            barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, barnUnder18Aar.getFnr());
        });
        // Log it with warning?
    }

}
