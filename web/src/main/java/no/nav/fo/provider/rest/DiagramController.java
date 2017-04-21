package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.*;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.PepClientInterface;
import no.nav.fo.service.SolrService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

@Api(value="Diagram")
@Path("/diagram")
@Produces(APPLICATION_JSON)
public class DiagramController {

    private static final Logger logger = getLogger(DiagramController.class);

    @Inject
    BrukertilgangService brukertilgangService;

    @Inject
    SolrService solrService;

    @Inject
    PepClientInterface pepClient;

    @POST
    public Response hentDiagramData(
            @QueryParam("veilederident") String veilederIdent,
            @QueryParam("enhet") String enhet,
            Filtervalg filtervalg) {

        try {
            String ident = SubjectHandler.getSubjectHandler().getUid();
            boolean brukerHarTilgangTilEnhet = brukertilgangService.harBrukerTilgang(ident, enhet);

            if (brukerHarTilgangTilEnhet) {

                if (filtervalg.ytelse == null) {
                    return Response.status(BAD_REQUEST).build();
                }

                Function<Bruker, Mapping> mapping = Bruker::getUtlopsdatoFasett;
                List<Mapping> mappings = asList(ManedMapping.values());
                if (filtervalg.ytelse == YtelseFilter.AAP_MAXTID) {
                    mapping = Bruker::getAapMaxtidFasett;
                    mappings = asList(KvartalMapping.values());
                }

                List<Bruker> brukere;
                if (isBlank(veilederIdent)) {
                    brukere = solrService.hentBrukereForEnhet(enhet, null, null, filtervalg);
                } else {
                    brukere = solrService.hentBrukereForVeileder(veilederIdent, enhet, null, null, filtervalg);
                }

                Map<Mapping, Long> gruppering = new LinkedHashMap<>();
                mappings.forEach((m) -> gruppering.put(m, 0L));

                Function<Bruker, Mapping> finalMapping = mapping;
                Map<Mapping, Long> brukergruppering = brukere
                        .stream()
                        .filter((bruker) -> finalMapping.apply(bruker) != null)
                        .collect(groupingBy(mapping, counting()));

                gruppering.putAll(brukergruppering);

                return Response.ok().entity(gruppering).build();
            } else {
                return Response.status(FORBIDDEN).build();
            }
        } catch (Exception e) {
            logger.warn("Kall mot upstream service feilet", e);
            return Response.status(BAD_GATEWAY).build();
        }
    }
}
