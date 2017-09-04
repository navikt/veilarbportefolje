package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.domene.*;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.SolrService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.provider.rest.RestUtils.createResponse;

@Api(value = "Diagram")
@Path("/diagram")
@Component
@Produces(APPLICATION_JSON)
public class DiagramController {

    private BrukertilgangService brukertilgangService;
    private SolrService solrService;

    @Inject
    public DiagramController(BrukertilgangService brukertilgangService, SolrService solrService) {
        this.brukertilgangService = brukertilgangService;
        this.solrService = solrService;
    }

    @POST
    public Response hentDiagramData(
            @QueryParam("veilederident") String veilederIdent,
            @QueryParam("enhet") String enhet,
            Filtervalg filtervalg) {

        return createResponse(() -> {
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, true);
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkFiltervalg(filtervalg);
            ValideringsRegler.harYtelsesFilter(filtervalg);
            TilgangsRegler.tilgangTilEnhet(brukertilgangService, enhet);

            Function<Bruker, Mapping> mapper = brukerMapping(filtervalg.ytelse);
            List<Mapping> alleFacetter = fasetter(filtervalg.ytelse);

            List<Bruker> brukere = solrService.hentBrukere(enhet, ofNullable(veilederIdent), null, null, filtervalg);

            Map<Mapping, Long> facetterteBrukere = brukere
                    .stream()
                    .filter((bruker) -> mapper.apply(bruker) != null)
                    .collect(groupingBy(mapper, counting()));

            alleFacetter.forEach((key) -> facetterteBrukere.putIfAbsent(key, 0L));

            return facetterteBrukere;
        });
    }

    private static Function<Bruker, Mapping> brukerMapping(YtelseFilter ytelse) {
        return ytelse == YtelseFilter.AAP_MAXTID ? Bruker::getAapMaxtidFasett : Bruker::getUtlopsdatoFasett;
    }

    private static List<Mapping> fasetter(YtelseFilter ytelse) {
        return (ytelse == YtelseFilter.AAP_MAXTID) ? asList(KvartalMapping.values()) : asList(ManedMapping.values());
    }
}
