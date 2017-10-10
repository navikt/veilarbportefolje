package no.nav.fo.service;


import no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import java.util.List;

import static no.nav.fo.config.CacheConfig.TILGANG_TIL_ENHET;

public class BrukertilgangService {

    @Inject
    VirksomhetEnhetService virksomhetEnhetService;

    @Cacheable(TILGANG_TIL_ENHET)
    public boolean harBrukerTilgang(String ident, String enhet) {
        return virksomhetEnhetService
                .hentEnheter(ident)
                .map(response -> finnesEnhetIListe(response.getEnhetListe(), enhet))
                .getOrElse(false);
    }

    private boolean finnesEnhetIListe(List<Enhet> enhetListe, String enhet) {
        return enhetListe.stream()
                .filter(item -> item.getEnhetId().equals(enhet))
                .toArray()
                .length > 0;
    }
}
