package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarnUnder18AarService {

    private final BarnUnder18AarRepository barnUnder18AarRepository;

    public Map<Fnr, List<BarnUnder18AarData>> hentBarnUnder18AarAlle(List<Fnr> fnrPersoner) {
        Map<Fnr, List<BarnUnder18AarData>> result = new HashMap<>();

        fnrPersoner.forEach(fnrPerson -> {
                    List<BarnUnder18AarData> barnListe = new ArrayList<>();
                    barnUnder18AarRepository.hentForeldreansvarForPerson(fnrPerson).forEach(fnrBarn ->
                            barnListe.add(barnUnder18AarRepository.hentInfoOmBarn(fnrBarn))
                    );
                    result.put(fnrPerson, barnListe);
                }
        );
        return result;
    }

    public List<Fnr> hentBarnFnrsForForeldre(List<Fnr> fnrPersoner) {
        List<Fnr> result = new ArrayList<>();

        fnrPersoner.forEach(fnrPerson -> {
                    result.addAll(barnUnder18AarRepository.hentForeldreansvarForPerson(fnrPerson));
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
                        slettBarnDataHvisIngenForeldreErUnderOppfolging(barnFnr);
                        secureLog.warn(String.format("Barn fjernet fra PDL for foreldre %s og barn %s", foresattIdent, barnFnr));
                    }
                }
        );

        barnFraPdl.forEach(barnUnder18Aar -> {
            barnUnder18AarRepository.lagreBarnData(barnUnder18Aar.getFnr(), barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());
            barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, barnUnder18Aar.getFnr());
        });
    }

    public void oppdaterEndringPaBarn(Fnr fnrBarn, LocalDate fodselsdato, String diskresjonskode) {
        List<BarnUnder18Aar> barn = new ArrayList<>();
        barnUnder18AarRepository.lagreBarnData(fnrBarn, );
    }


    public void slettBarnDataHvisIngenForeldreErUnderOppfolging(List<Fnr> barnIdenter) {
        barnIdenter.forEach(this::slettBarnDataHvisIngenForeldreErUnderOppfolging);
    }

    private void slettBarnDataHvisIngenForeldreErUnderOppfolging(Fnr barnFnr) {
        if (!barnUnder18AarRepository.finnesBarnIForeldreansvar(barnFnr)) {
            barnUnder18AarRepository.slettBarnData(barnFnr);
        }
    }

    public boolean erFnrBarnAvForelderUnderOppfolging(List<Fnr> fnrs) {
        return barnUnder18AarRepository.finnesBarnIForeldreansvar(fnrs);
    }

}
