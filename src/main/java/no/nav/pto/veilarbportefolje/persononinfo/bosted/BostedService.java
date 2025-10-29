package no.nav.pto.veilarbportefolje.persononinfo.bosted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class BostedService {
    private final BostedRepository bostedRepository;
    private final KodeverkService kodeverkService;

    private List<Bydel> hentBydel(String enhetId) {
        List<String> bydelCodes = bostedRepository.hentBydel(enhetId);
        List<Bydel> codeToBydel = new ArrayList<>();

        bydelCodes.stream()
                .forEach(code -> {
                    String bydelFulltNavn = kodeverkService.getBeskrivelseForBydeler(code);
                    if (bydelFulltNavn != null && !bydelFulltNavn.isEmpty()) {
                        codeToBydel.add(new Bydel(code, bydelFulltNavn));
                    }
                });
        return codeToBydel.stream()
                .sorted(Comparator.comparing(Bydel::getNavn, Collator.getInstance(new Locale("nob", "NO"))))
                .toList();
    }

    private List<Kommune> hentKommune(String enhetId) {
        List<String> kommuneCodes = bostedRepository.hentKommune(enhetId);
        List<Kommune> codeToKommune = new ArrayList<>();

        kommuneCodes.stream()
                .forEach(code -> {
                    String komunneFulltNavn = kodeverkService.getBeskrivelseForKomunner(code);
                    if (komunneFulltNavn != null && !komunneFulltNavn.isEmpty()) {
                        codeToKommune.add(new Kommune(code, komunneFulltNavn));
                    }
                });
        return codeToKommune.stream()
                .sorted(Comparator.comparing(Kommune::getNavn, Collator.getInstance(new Locale("nob", "NO"))))
                .toList();
    }

    public List<GeografiskBosted> hentGeografiskBosted(String enhetId) {
        List<GeografiskBosted> result = new ArrayList<>();
        List<Kommune> kommunes = hentKommune(enhetId);
        List<Bydel> bydels = hentBydel(enhetId);

        kommunes.forEach(kommune -> result.add(new GeografiskBosted(kommune.code, kommune.code + " " + kommune.navn)));
        bydels.forEach(bydel -> result.add(new GeografiskBosted(bydel.code, bydel.code + " " + bydel.navn)));

        return result.stream()
                .sorted(Comparator.comparing(GeografiskBosted::getCode))
                .toList();

    }
}
