package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.domene.*;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.StepperUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.provider.rest.RestUtils.createResponse;

@Api(value = "Diagram")
@Path("/diagram")
@Component
@Produces(APPLICATION_JSON)
public class DiagramController {

    private PepClient pepClient;
    private SolrService solrService;

    @Inject
    public DiagramController(PepClient pepClient, SolrService solrService) {
        this.pepClient = pepClient;
        this.solrService = solrService;
    }

    @POST
    @Path("/v2")
    public Response hentDiagramData(
            @QueryParam("veilederident") String veilederIdent,
            @QueryParam("enhet") String enhet,
            Filtervalg filtervalg
    ) {
        return createResponse(() -> {
            ValideringsRegler.sjekkVeilederIdent(veilederIdent, true);
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkFiltervalg(filtervalg);
            ValideringsRegler.harYtelsesFilter(filtervalg);
            TilgangsRegler.tilgangTilEnhet(pepClient, enhet);

            Optional<StepperFacetConfig> stepperConfig = StepperFacetConfig.stepperFacetConfig(filtervalg.ytelse);
            if (stepperConfig.isPresent()) {
                BrukereMedAntall brukere = solrService.hentBrukere(enhet, ofNullable(veilederIdent), null, null, filtervalg);
                return StepperUtils.groupByStepping(stepperConfig.get(), brukere.getBrukere(), brukerUkeMapping(stepperConfig.get().getYtelse()));
            } else {
                BrukereMedAntall brukereMedAntall = solrService.hentBrukere(enhet, ofNullable(veilederIdent), null, null, filtervalg);

                Map<ManedFasettMapping, Long> grupperte = brukereMedAntall
                        .getBrukere()
                        .stream()
                        .filter((bruker) -> bruker.getUtlopsdatoFasett() != null)
                        .map(Bruker::getUtlopsdatoFasett)
                        .collect(groupingBy(Function.identity(), counting()));

                stream(ManedFasettMapping.values()).forEach((key) -> grupperte.putIfAbsent(key, 0L));

                return grupperte
                        .entrySet()
                        .stream()
                        .map((entry) -> {
                            int gruppe = entry.getKey().ordinal() + 1;
                            return new StepperUtils.Step(gruppe, gruppe, entry.getValue());
                        })
                        .collect(toList());
            }
        });
    }

    private static Function<Bruker, Integer> brukerUkeMapping(YtelseFilter ytelse) {
        if (ytelse == YtelseFilter.AAP_MAXTID) {
            return Bruker::getAapmaxtidUke;
        } else if (ytelse == YtelseFilter.AAP_UNNTAK) {
            return Bruker::getAapUnntakUkerIgjen;
        } else if (ytelse == YtelseFilter.DAGPENGER_MED_PERMITTERING) {
            return Bruker::getPermutlopUke;
        } else if (ytelse == YtelseFilter.DAGPENGER ||
                ytelse == YtelseFilter.ORDINARE_DAGPENGER ||
                ytelse == YtelseFilter.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI ||
                ytelse == YtelseFilter.LONNSGARANTIMIDLER_DAGPENGER) {
            return Bruker::getDagputlopUke;
        }
        return (Bruker bruker) -> -1;
    }
}
