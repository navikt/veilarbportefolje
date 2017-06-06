package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Api(value = "Fjern brukere")
@Path("/bruker")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class FjernbrukereController {

    @Inject
    private PepClient pepClient;

    @Inject
    private SolrService solrService;

    @POST
    @Path("/fjernbrukere")
    public Response fjernBrukere(List<String> fnrs) {
        return RestUtils.createResponse(() -> {
            TilgangsRegler.tilgangTilOppfolging(pepClient);

            fnrs.forEach((fnr) -> {
                TilgangsRegler.tilgangTilBruker(pepClient, fnr);
                solrService.slettBruker(fnr);
            });

            solrService.commit();

            return "OK";
        });
    }
}
