package no.nav.fo.veilarbportefolje.provider.rest;

import io.swagger.annotations.Api;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.OppfolgingEnhetDTO;
import no.nav.fo.veilarbportefolje.domene.OppfolgingEnhetPageDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.brukerdialog.security.domain.IdentType.Systemressurs;

@Api(value = "OppfolgingEnhet")
@Path("/oppfolgingenhet")
@Component
@Produces(APPLICATION_JSON)
public class OppfolgingenhetRessurs {

    static final int PAGE_SIZE_MAX = 1000;
    private static final int PAGE_NUMBER_MAX = 500_000;

    private BrukerRepository brukerRepository;

    @Inject
    public OppfolgingenhetRessurs(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    @GET
    public OppfolgingEnhetPageDTO getOppfolgingEnhet(@DefaultValue("1") @QueryParam("page_number") int pageNumber, @DefaultValue("10") @QueryParam("page_size") int pageSize) {

        autoriserBruker();

        Integer totalNumberOfUsers = brukerRepository.hentAntallBrukereUnderOppfolging().orElseThrow(() -> new WebApplicationException(503));
        int totalNumberOfPages = totalNumberOfUsers / pageSize;

        validatePageSize(pageSize);
        validatePageNumber(pageNumber, totalNumberOfPages);

        List<OppfolgingEnhetDTO> brukereMedOppfolgingsEnhet = brukerRepository.hentBrukereUnderOppfolging(pageNumber, pageSize);

        Integer nextPage = totalNumberOfPages <= pageNumber ? null : pageNumber + 1;

        return new OppfolgingEnhetPageDTO(pageNumber, nextPage, totalNumberOfPages, brukereMedOppfolgingsEnhet);
    }

    private void autoriserBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElseThrow(NotFoundException::new);
        String ident = SubjectHandler.getIdent().orElseThrow(NotFoundException::new);

        if (ugyldigIdent(identType, ident)) {
            throw new NotFoundException();
        }
    }

    static boolean ugyldigIdent(IdentType identType, String ident) {
        return !identType.equals(Systemressurs) || !"srvveilarboppfolging".equals(ident);
    }

    static void validatePageNumber(int pageNumber, int pagesTotal) {

        if (pageNumber < 1) {
            throw new WebApplicationException("Page number is below 1", 400);
        }

        if (pageNumber > pagesTotal) {
            throw new WebApplicationException("Page number is higher than total number of pages", 404);
        }

        if (pageNumber > PAGE_NUMBER_MAX) {
            throw new WebApplicationException("Page number exceeds max limit", 400);
        }
    }

    static void validatePageSize(int pageSize) {

        if (pageSize < 1) {
            throw new WebApplicationException("Page size too small", 400);
        }

        if (pageSize > PAGE_SIZE_MAX) {
            throw new WebApplicationException("Page size exceeds max limit", 400);
        }
    }

}
