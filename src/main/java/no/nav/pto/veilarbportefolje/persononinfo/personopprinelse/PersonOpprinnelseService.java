package no.nav.pto.veilarbportefolje.persononinfo.personopprinelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.util.StringUtils;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonOpprinnelseService {
    private final PersonOpprinnelseRepository personOpprinnelseRepository;
    private final KodeverkService kodeverkService;

    public List<Foedeland> hentFoedeland(String enhetId) {
        List<String> landCodes = personOpprinnelseRepository.hentFoedeland(enhetId);
        List<Foedeland> codeToLand = new ArrayList<>();

        landCodes.stream()
                .forEach(code -> {
                    String foedelandFulltNavn = kodeverkService.getBeskrivelseForLandkode(code);
                    if (foedelandFulltNavn != null && !foedelandFulltNavn.isEmpty()) {
                        codeToLand.add(new Foedeland(code, StringUtils.capitalize(foedelandFulltNavn)));
                    }
                });
        return codeToLand.stream()
                .sorted(Comparator.comparing(Foedeland::getLand, Collator.getInstance(new Locale("nob", "NO"))))
                .toList();
    }

    public List<TolkSpraak> hentTolkSpraak(String enhetId) {
        List<TolkSpraak> tolkSpraak = new ArrayList<>();
        personOpprinnelseRepository.hentTolkSpraak(enhetId)
                .stream()
                .filter(tolkSpraakEnhet -> tolkSpraakEnhet != null && !tolkSpraakEnhet.isEmpty())
                .sorted()
                .forEach(code -> {
                    String spraak = kodeverkService.getBeskrivelseForSpraakKode(code);
                    if (spraak != null && !spraak.isEmpty()) {
                        tolkSpraak.add(new TolkSpraak(code, StringUtils.capitalize(spraak)));
                    }
                });
        return tolkSpraak.stream()
                .sorted(Comparator.comparing(TolkSpraak::getSpraak, Collator.getInstance(new Locale("nob", "NO"))))
                .toList();
    }
}
