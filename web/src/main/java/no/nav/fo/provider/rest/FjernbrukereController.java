package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Api(value = "Fjern brukere")
@Path("/bruker")
@Component
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class FjernbrukereController {

    @Inject
    private PepClient pepClient;

    @Inject
    private SolrService solrService;

    @POST
    @Path("/fjern")
    public Response fjernBrukere(List<String> fnrs) {
        return RestUtils.createResponse(() -> {
            String veilederIdent = SubjectHandler.getSubjectHandler().getUid();
            TilgangsRegler.tilgangTilOppfolging(pepClient);

            solrService.query("veileder_id:" + veilederIdent)
                    .map((brukere) -> brukere.stream().filter((bruker) -> fnrs.contains(bruker.getFnr())).collect(toList()))
                    .forEach((brukere) -> brukere.forEach((bruker) -> solrService.slettBruker(bruker.getFnr())));

            solrService.commit();

            return "OK";
        });
    }
}
