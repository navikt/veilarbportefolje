package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarnUnder18AarService {

    private final BarnUnder18AarRepository barnUnder18AarRepository;

    public Map<Fnr, List<BarnUnder18AarData>> hentBarnUnder18AarAlle(List<Fnr> fnrPersoner) {
        Map<Fnr, List<BarnUnder18AarData>> result = new HashMap<>();
        List<BarnUnder18AarData> barnListe = new ArrayList();

        fnrPersoner.forEach(fnrPerson -> {
                    barnListe.clear();
                    barnUnder18AarRepository.hentForeldreansvarForPerson(fnrPerson).forEach(fnrBarn ->
                            {
                                barnListe.add(barnUnder18AarRepository.hentInfoOmBarn(fnrBarn));
                            }
                    );
                    result.put(fnrPerson, barnListe);
                }
        );
        return result;
    }

    public void lagreBarnOgForeldreansvar(Fnr foresattIdent, List<BarnUnder18Aar> barnFraPdl) {
        List<Fnr> lagredeBarn = barnUnder18AarRepository.hentForeldreansvarForPerson(foresattIdent);

        List<Fnr> barnFnrFraPdl = barnFraPdl.stream().map(BarnUnder18Aar::getFnr).toList();

        lagredeBarn.forEach(barnFnr -> {
                    if (!barnFnrFraPdl.contains(barnFnr)) {
                        barnUnder18AarRepository.slettForeldreansvar(foresattIdent, barnFnr);
                        if (!barnUnder18AarRepository.finnesBarnIForeldreansvar(barnFnr)) {
                            barnUnder18AarRepository.slettBarnData(barnFnr);
                        }
                    }
                }
        );

        barnFraPdl.forEach(barnUnder18Aar -> {
            barnUnder18AarRepository.lagreBarnData(barnUnder18Aar.getFnr(), barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());
            barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, barnUnder18Aar.getFnr());
        });
        // Log it with warning?
    }

}
