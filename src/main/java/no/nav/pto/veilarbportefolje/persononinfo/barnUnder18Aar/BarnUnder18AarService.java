package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        // Get existing barn in table foreldreansvar for foresatt
        List<Fnr> lagredeBarn = barnUnder18AarRepository.hentForeldreansvarForPerson(foresattIdent);

        List<Fnr> barnFnrFraPdl = barn.stream().map(BarnUnder18Aar::getFnr).toList();

        lagredeBarn.forEach( barnFnr -> {

                    if (!barnFnrFraPdl.contains(barnFnr)){
                        //delete foreldreansvar
                        // Check if this child exists in foreldreansvar with someone else than foresatt
                        // If no: remove from bruker_data_barn
                        // If yes:  keep in bruker_data_barn (do nothing)
                    }
                }

        );

        barn.forEach(barnUnder18Aar -> {
            barnUnder18AarRepository.lagreBarnData(barnUnder18Aar.getFnr(), barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());
            barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, barnUnder18Aar.getFnr());
        });

        // 1a. If nothing has changed or only new childre, proceed
        if (lagredeBarn.isEmpty()){
            barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, );
            barnUnder18AarRepository.lagreBarnData(barn, barnFodselsdato, barnDiskresjonskode);
        }else if (lagredeBarn.contains(barnIdent)){
            barnUnder18AarRepository.lagreBarnData(barnIdent, barnFodselsdato, barnDiskresjonskode);
        }else{


        }
        // 1b. If one or more children is missing, do the following:
        // Log it with warning

        // remove link between foresatt and barn




    }

}
