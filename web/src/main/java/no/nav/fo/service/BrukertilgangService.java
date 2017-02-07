package no.nav.fo.service;


import no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet;

import javax.inject.Inject;
import java.util.List;

public class BrukertilgangService {

    @Inject
    VirksomhetEnhetService virksomhetEnhetService;

    public boolean harBrukerTilgang(String ident, String enhet) {
        return virksomhetEnhetService
                .hentEnheter(ident)
                .map(response -> finnesEnhetIListe(response.getEnhetListe(), enhet))
                .getOrElse(false);
    }

    private boolean finnesEnhetIListe(List<Enhet> enhetListe, String enhet) {
        return enhetListe.stream()
                    .filter( item  -> item.getEnhetId().equals(enhet))
                    .toArray()
                    .length > 0;
    }
}
