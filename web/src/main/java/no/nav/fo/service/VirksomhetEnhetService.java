package no.nav.fo.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;

import javax.inject.Inject;

@Slf4j
public class VirksomhetEnhetService {

    @Inject
    private Enhet virksomhetEnhet;

    public Try<WSHentEnhetListeResponse> hentEnheter(String ident) {
        WSHentEnhetListeRequest request = new WSHentEnhetListeRequest();
        request.setRessursId(ident);
        return Try.of(() -> virksomhetEnhet.hentEnhetListe(request))
                .onFailure(e -> log.warn(String.format("Kunne ikke hente ansattopplysninger for %s", ident), e));
    }
}
