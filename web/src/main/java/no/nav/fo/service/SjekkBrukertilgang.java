package no.nav.fo.service;


import no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeResponse;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class SjekkBrukertilgang {

    private static final Logger logger =  getLogger(SjekkBrukertilgang.class);

    @Inject
    VirksomhetEnhetServiceImpl virksomhetEnhetService;

    public boolean harBrukerTilgangTilEnhet(String ident, String enhet) throws Exception {
        WSHentEnhetListeRequest request = new WSHentEnhetListeRequest();
        request.setRessursId(ident);

        try {
            logger.debug(String.format("Sjekker om %s har tilgang til enhet %S", ident, enhet));
            WSHentEnhetListeResponse response = virksomhetEnhetService.hentEnhetListe(ident);
            Boolean harBrukerTilgang = finnesEnhetIListe(response.getEnhetListe(), enhet);
            if(!harBrukerTilgang){
                String message = String.format("Bruker %s har ikke tilgang til enhet", ident, enhet);
                logger.warn(message);
            }
            return harBrukerTilgang;
        } catch (Exception e) {
            throw e;
        }
    }

    private boolean finnesEnhetIListe(List<Enhet> enhetListe, String enhet) {
        return enhetListe.stream()
                    .filter( item  -> item.getEnhetId().equals(enhet))
                    .toArray()
                    .length > 0;
    }
}
