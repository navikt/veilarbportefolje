package no.nav.pto.veilarbportefolje.api;

import io.swagger.annotations.Api;
import no.nav.pto.veilarbportefolje.database.PersonRepository;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import no.nav.pto.veilarbportefolje.abac.PepClient;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Api(value = "Personinfo")
@Path("/personinfo")
@Component
@Produces(APPLICATION_JSON)
public class PersoninfoController {
    private PepClient pepClient;
    private PersonRepository personRepository;

    @Inject
    public PersoninfoController(PepClient pepClient, PersonRepository personRepository) {
        this.pepClient = pepClient;
        this.personRepository = personRepository;
    }

    @GET
    @Path("/{fnr}")
    public Personinfo hentPersoninfo(@PathParam("fnr") String fnr) {
        TilgangsRegler.tilgangTilBruker(pepClient, fnr);
        return personRepository.hentPersoninfoForFnr(Fnr.of(fnr))
                .orElseThrow(() -> new NotFoundException("Kunne ikke finne personinfo for: " + fnr));
    }
}
