package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.DateUtils.erUnder18Aar;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarnUnder18AarService {

    private final BarnUnder18AarRepository barnUnder18AarRepository;

    public Map<Fnr, List<BarnUnder18AarData>> hentBarnUnder18Aar(List<Fnr> fnrForeldre) {
        Map<Fnr, List<BarnUnder18AarData>> result = new HashMap<>();

        fnrForeldre.forEach(fnrPerson -> {
                    List<BarnUnder18AarData> barnListe = new ArrayList<>();
                    barnUnder18AarRepository.hentForeldreansvarForPerson(fnrPerson).forEach(fnrBarn -> {
                                BarnUnder18AarData barnUnder18AarData = barnUnder18AarRepository.hentInfoOmBarn(fnrBarn);
                                if (barnUnder18AarData != null) {
                                    barnListe.add(barnUnder18AarData);
                                }
                            }
                    );
                    result.put(fnrPerson, barnListe);
                }
        );
        return result;
    }

    public List<Fnr> hentBarnFnrsForForeldre(List<Fnr> fnrForeldre) {
        List<Fnr> result = new ArrayList<>();

        fnrForeldre.forEach(fnrForelder -> {
                    result.addAll(barnUnder18AarRepository.hentForeldreansvarForPerson(fnrForelder));
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
            if (erUnder18Aar(barnUnder18Aar.getFodselsdato())) {
                barnUnder18AarRepository.lagreBarnData(barnUnder18Aar.getFnr(), barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());
                barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, barnUnder18Aar.getFnr());
            }
        });
    }


    public void oppdaterEndringPaBarn(Fnr fnrBarn, PDLPersonBarn pdlPersonBarn) {
        barnUnder18AarRepository.lagreBarnData(fnrBarn, pdlPersonBarn.getFodselsdato(), pdlPersonBarn.getDiskresjonskode());
    }

    public void slettBarnDataHvisIngenForeldreErUnderOppfolging(List<Fnr> barnIdenter) {
        barnIdenter.forEach(this::slettBarnDataHvisIngenForeldreErUnderOppfolging);
    }

    private void slettBarnDataHvisIngenForeldreErUnderOppfolging(Fnr barnFnr) {
        if (!barnUnder18AarRepository.finnesBarnIForeldreansvar(barnFnr)) {
            barnUnder18AarRepository.slettBarnData(barnFnr);
        }
    }

    public boolean erFnrBarnAvForelderUnderOppfolging(List<Fnr> fnrBarn) {
        return barnUnder18AarRepository.finnesBarnIForeldreansvar(fnrBarn);
    }

    public List<Fnr> finnForeldreTilBarn(Fnr fnrBarn) {
        return barnUnder18AarRepository.hentForeldreTilBarn(fnrBarn);
    }

    public void slettDataForBarnSomErOver18() {
        List<Fnr> fnrBarnOver18 = barnUnder18AarRepository.hentAlleBarnOver18();

        fnrBarnOver18.forEach(fnrBarn -> {
            barnUnder18AarRepository.slettForeldreansvar(fnrBarn);
            barnUnder18AarRepository.slettBarnData(fnrBarn);
        });

    }

    public void handterBarnIdentEndring(Fnr aktivtFnrBarn, List<Fnr> inaktiveFnrs) {
        if (!inaktiveFnrs.isEmpty()) {
            barnUnder18AarRepository.oppdatereBarnIdent(aktivtFnrBarn, inaktiveFnrs);
        }
    }

    public void handterForeldreIdentEndring(Fnr aktivtFnr, List<Fnr> inaktiveFnr) {
        if (!inaktiveFnr.isEmpty()) {
            barnUnder18AarRepository.oppdatereForeldreIdent(aktivtFnr, inaktiveFnr);
        }
    }
}
