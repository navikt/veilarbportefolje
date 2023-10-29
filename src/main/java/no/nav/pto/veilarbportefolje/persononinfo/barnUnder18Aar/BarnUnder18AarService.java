package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.util.DateUtils.erUnder18Aar;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BarnUnder18AarService {

    private final BarnUnder18AarRepository barnUnder18AarRepository;
    private final PdlPortefoljeClient pdlClient;

    private final Cache<List<Fnr>, Boolean> cacheErFnrBarnAvForelderUnderOppfolging = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(50000)
            .build();

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

    public void lagreBarnOgForeldreansvar(Fnr foresattIdent, List<Fnr> foreldreansvarPDL) {
        List<Fnr> nyeBarn = List.of();
        try {
            List<Fnr> lagredeBarn = barnUnder18AarRepository.hentForeldreansvarForPerson(foresattIdent);

            lagredeBarn.forEach(barnFnr -> {
                if (foreldreansvarPDL == null || !foreldreansvarPDL.contains(barnFnr)) {
                    secureLog.warn("Slett foreldreansvar");
                    slettForeldreansvar(foresattIdent, barnFnr);
                    slettBarnDataHvisIngenForeldreErUnderOppfolging(barnFnr);
                }
            });

            if (foreldreansvarPDL != null && !foreldreansvarPDL.isEmpty()) {
                nyeBarn = foreldreansvarPDL.stream().filter(barnFnr -> !lagredeBarn.contains(barnFnr)).toList();
                getDataFromPDLAndInsertBarn(foresattIdent, nyeBarn);
            }
        } catch (Exception e) {
            secureLog.error("Kan ikke lagre data om barn og foreldreansvar for person: " + foresattIdent + ". Antall barn: " + nyeBarn.size() + ", error: " + e.getMessage(), e);
            throw new RuntimeException("Kan ikke lagre data om barn");
        }
    }


    public void getDataFromPDLAndInsertBarn(Fnr foresattIdent, List<Fnr> nyeBarn) {
        if (nyeBarn.isEmpty()) {
            return;
        }

        Map<Fnr, PDLPersonBarn> pdlPersonBarn = pdlClient.hentBrukerBarnDataBolkFraPdl(nyeBarn);
        if (pdlPersonBarn != null && !pdlPersonBarn.isEmpty()) {
            pdlPersonBarn.forEach((fnrBarn, dataBarn) -> {
                if (dataBarn.isErIlive() && erUnder18Aar(dataBarn.getFodselsdato())) {
                    barnUnder18AarRepository.lagreBarnData(fnrBarn, dataBarn.getFodselsdato(), dataBarn.getDiskresjonskode());
                    barnUnder18AarRepository.lagreForeldreansvar(foresattIdent, fnrBarn);
                } else {
                    slettForeldreansvar(foresattIdent, fnrBarn);
                    slettBarnDataHvisIngenForeldreErUnderOppfolging(fnrBarn);
                }
            });
        }
    }


    public void oppdaterEndringPaBarn(Fnr fnrBarn, PDLPersonBarn pdlPersonBarn) {
        barnUnder18AarRepository.lagreBarnData(fnrBarn, pdlPersonBarn.getFodselsdato(), pdlPersonBarn.getDiskresjonskode());
    }

    public void slettBarnDataHvisIngenForeldreErUnderOppfolging(List<Fnr> barnIdenter) {
        barnIdenter.forEach(this::slettBarnDataHvisIngenForeldreErUnderOppfolging);
    }

    private void slettForeldreansvar(Fnr foresattIdent, Fnr barnFnr) {
        boolean foreldreansvarSlettet = barnUnder18AarRepository.slettForeldreansvar(foresattIdent, barnFnr);
        if (foreldreansvarSlettet) {
            secureLog.warn(String.format("Barn fjernet fra PDL for foreldre %s og barn %s", foresattIdent, barnFnr));
        }
    }

    private void slettBarnDataHvisIngenForeldreErUnderOppfolging(Fnr barnFnr) {
        if (!barnUnder18AarRepository.finnesBarnIForeldreansvar(barnFnr)) {
            secureLog.info(String.format("Sletter data om barn %s fra bruker_data_barn siden de ikke lenger eksisterer i foreldreansvar", barnFnr));
            barnUnder18AarRepository.slettBarnData(barnFnr);
        }
    }

    public boolean erFnrBarnAvForelderUnderOppfolging(List<Fnr> fnrBarn) {
        if (fnrBarn == null || fnrBarn.isEmpty()) {
            return false;
        }
        return tryCacheFirst(cacheErFnrBarnAvForelderUnderOppfolging, fnrBarn,
                () -> barnUnder18AarRepository.finnesBarnIForeldreansvar(fnrBarn));
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

}
