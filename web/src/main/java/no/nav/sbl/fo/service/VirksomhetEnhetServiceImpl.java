package no.nav.sbl.fo.service;

import no.nav.virksomhet.tjenester.enhet.meldinger.v1.HentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.HentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.binding.Enhet;
import no.nav.virksomhet.tjenester.enhet.v1.binding.HentEnhetListeRessursIkkeFunnet;
import no.nav.virksomhet.tjenester.enhet.v1.binding.HentEnhetListeUgyldigInput;
import org.slf4j.Logger;
import java.lang.Exception;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class VirksomhetEnhetServiceImpl {

    private static final Logger logger = getLogger(VirksomhetEnhetServiceImpl.class);

    @Inject
    private Enhet virksomhetEnhet;

    public HentEnhetListeResponse hentEnhetListe(String ident) throws Exception{

        try {
            HentEnhetListeRequest request = new HentEnhetListeRequest();
            request.setRessursId(ident);
            HentEnhetListeResponse response = virksomhetEnhet.hentEnhetListe(request);
            return response;
        } catch (HentEnhetListeUgyldigInput e) {
            String feil = String.format("Kunne ikke hente ansattopplysnigner for %s", ident);
            logger.error(feil, e);
            throw new Exception(feil ,e);
        } catch (HentEnhetListeRessursIkkeFunnet e) {
            String feil = String.format("Kunne ikke hente ansattopplysnigner for %s", ident);
            logger.error(feil,e);
            throw new Exception(e);
        } catch (java.lang.Exception e) {
            String feil = String.format("Kunne ikke hente ansattopplysnigner for %s: Ukjent Feil", ident);
            logger.error(feil, e);
            throw new Exception(e);
        }
    }
}
