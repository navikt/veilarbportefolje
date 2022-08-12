package no.nav.pto.veilarbportefolje.persononinfo.personopprinelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Foedeland;
import no.nav.pto.veilarbportefolje.domene.TolkSpraak;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonOpprinnelseService {
    private final PersonOpprinnelseRepository personOpprinnelseRepository;
    private final KodeverkService kodeverkService;

    public List<Foedeland> hentFoedeland() {
        List<String> landCodes = personOpprinnelseRepository.hentFoedeland();
        List<Foedeland> codeToLand = new ArrayList<>();

        landCodes.stream()
                .forEach(code -> {
                    String foedelandFulltNavn = kodeverkService.getBeskrivelseForLandkode(code);
                    if (foedelandFulltNavn != null && !foedelandFulltNavn.isEmpty()) {
                        codeToLand.add(new Foedeland(code, StringUtils.capitalize(foedelandFulltNavn)));
                    }
                });
        codeToLand.sort(Comparator.comparing(Foedeland::getLand));
        return codeToLand;
    }

    public List<TolkSpraak> hentTolkSpraak() {
        List<TolkSpraak> tolkSpraak = new ArrayList<>();
        personOpprinnelseRepository.hentTolkSpraak()
                .stream()
                .filter(x -> x != null && !x.isEmpty())
                .sorted()
                .forEach(code -> {
                    String spraak = kodeverkService.getBeskrivelseForSpraakKode(code);
                    if (spraak != null && !spraak.isEmpty()) {
                        tolkSpraak.add(new TolkSpraak(code, StringUtils.capitalize(spraak)));
                    }
                });
        return tolkSpraak;
    }
}
