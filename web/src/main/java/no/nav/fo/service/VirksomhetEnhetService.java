package no.nav.fo.service;

import javaslang.control.Try;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import no.nav.virksomhet.tjenester.enhet.v1.HentEnhetListeRessursIkkeFunnet;
import no.nav.virksomhet.tjenester.enhet.v1.HentEnhetListeUgyldigInput;
import org.slf4j.Logger;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class VirksomhetEnhetService {

    private static final Logger logger = getLogger(VirksomhetEnhetService.class);

    @Inject
    private Enhet virksomhetEnhet;

    public WSHentEnhetListeResponse hentEnhetListe(String ident) throws Exception{

        try {
            WSHentEnhetListeRequest request = new WSHentEnhetListeRequest();
            request.setRessursId(ident);
            WSHentEnhetListeResponse response = virksomhetEnhet.hentEnhetListe(request);
            return response;
        } catch (HentEnhetListeUgyldigInput e) {
            String feil = String.format("Kunne ikke hente ansattopplysnigner for %s", ident);
            logger.error(feil, e);
            throw e;
        } catch (HentEnhetListeRessursIkkeFunnet e) {
            String feil = String.format("Kunne ikke hente ansattopplysnigner for %s", ident);
            logger.error(feil,e);
            throw e;
        } catch (java.lang.Exception e) {
            String feil = String.format("Kunne ikke hente ansattopplysnigner for %s: Ukjent Feil", ident);
            logger.error(feil, e);
            throw e;
        }
    }

    public Try<WSHentEnhetListeResponse> hentEnheter(String ident) {
        WSHentEnhetListeRequest request = new WSHentEnhetListeRequest();
        request.setRessursId(ident);
        return Try.of(() -> virksomhetEnhet.hentEnhetListe(request))
                .onFailure(e -> logger.error(String.format("Kunne ikke hente ansattopplysninger for %s", ident), e));
    }
}
