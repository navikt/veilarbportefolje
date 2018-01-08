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

            Function<Bruker, FasettMapping> mapper = brukerMapping(filtervalg.ytelse);
            List<FasettMapping> alleFacetter = fasetter(filtervalg.ytelse);

            BrukereMedAntall brukereMedAntall = solrService.hentBrukere(enhet, ofNullable(veilederIdent), null, null, filtervalg);

            Map<FasettMapping, Long> facetterteBrukere = brukereMedAntall.getBrukere()
                    .stream()
                    .filter((bruker) -> mapper.apply(bruker) != null)
                    .collect(groupingBy(mapper, counting()));

            alleFacetter.forEach((key) -> facetterteBrukere.putIfAbsent(key, 0L));

            return facetterteBrukere;
        });
    }

    private static Function<Bruker, FasettMapping> brukerMapping(YtelseFilter ytelse) {
        if (ytelse == YtelseFilter.AAP_MAXTID) {
            return Bruker::getAapmaxtidUkeFasett;
        } else if (ytelse == YtelseFilter.AAP_UNNTAK) {
            return Bruker::getAapUnntakUkerIgjenFasett;
        } else if (ytelse == YtelseFilter.DAGPENGER_MED_PERMITTERING) {
            return Bruker::getPermutlopUkeFasett;
        } else if (ytelse == YtelseFilter.DAGPENGER || ytelse == YtelseFilter.DAGPENGER_OVRIGE || ytelse == YtelseFilter.ORDINARE_DAGPENGER) {
            return Bruker::getDagputlopUkeFasett;
        }
        return Bruker::getUtlopsdatoFasett;
    }

    private static List<FasettMapping> fasetter(YtelseFilter ytelse) {
        if (ytelse == YtelseFilter.AAP_MAXTID) {
            return asList(AAPMaxtidUkeFasettMapping.values());
        } else if (ytelse == YtelseFilter.AAP_UNNTAK ){
            return asList(AAPUnntakUkerIgjenFasettMapping.values());
        } else if (ytelse == YtelseFilter.DAGPENGER || ytelse == YtelseFilter.DAGPENGER_OVRIGE || ytelse == YtelseFilter.ORDINARE_DAGPENGER || ytelse == YtelseFilter.DAGPENGER_MED_PERMITTERING) {
            return asList(DagpengerUkeFasettMapping.values());
        }
        return asList(ManedFasettMapping.values());
    }
}
