package no.nav.fo.veilarbportefolje.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.util.StepperUtils;
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
import static no.nav.fo.veilarbportefolje.provider.rest.RestUtils.createResponse;

@Api(value = "Diagram")
@Path("/diagram")
@Component
@Produces(APPLICATION_JSON)
public class DiagramController {

    private PepClient pepClient;
    private ElasticIndexer elasticIndexer;

    @Inject
    public DiagramController(PepClient pepClient, ElasticIndexer indekseringService) {
        this.pepClient = pepClient;
        this.elasticIndexer = indekseringService;
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
                BrukereMedAntall brukere = elasticIndexer.hentBrukere(enhet, ofNullable(veilederIdent), "ikke_satt", "ikke_satt", filtervalg);
                return StepperUtils.groupByStepping(stepperConfig.get(), brukere.getBrukere(), brukerUkeMapping(stepperConfig.get().getYtelse()));
            } else {
                BrukereMedAntall brukereMedAntall = elasticIndexer.hentBrukere(enhet, ofNullable(veilederIdent), "ikke_satt", "ikke_satt", filtervalg);

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
                ytelse == YtelseFilter.DAGPENGER_OVRIGE ||
                ytelse == YtelseFilter.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI ||
                ytelse == YtelseFilter.LONNSGARANTIMIDLER_DAGPENGER) {
            return Bruker::getDagputlopUke;
        }
        return (Bruker bruker) -> -1;
    }
}
