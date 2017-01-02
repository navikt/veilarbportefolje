package no.nav.fo.provider.rest;

import no.nav.fo.service.VirksomhetEnhetServiceImpl;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.HentEnhetListeResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Path("/hentenheter")
@Produces(APPLICATION_JSON)
public class HentEnheterController {

    @Inject
    VirksomhetEnhetServiceImpl virksomhetEnhetService;

    @Context
    SecurityContext securityContex;

    @GET
    public Map<String, HentEnhetListeResponse> hentEnheter() {
        HentEnhetListeResponse response = null;
        String ident = securityContex.getUserPrincipal().getName();

        try {
            response = virksomhetEnhetService.hentEnhetListe(ident);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, HentEnhetListeResponse> map = new HashMap<>();
        map.put(ident, response);
        return map;
    }
}
