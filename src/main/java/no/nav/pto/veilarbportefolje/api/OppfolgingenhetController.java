package no.nav.pto.veilarbportefolje.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.OppfolgingEnhetDTO;
import no.nav.pto.veilarbportefolje.domene.OppfolgingEnhetPageDTO;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.auth.subject.IdentType.InternBruker;
import static no.nav.common.auth.subject.IdentType.Systemressurs;

@Slf4j
@RestController
@RequestMapping("/api/oppfolgingenhet")
@Produces(APPLICATION_JSON)
public class OppfolgingenhetController {

    static final int PAGE_SIZE_MAX = 1000;
    private static final int PAGE_NUMBER_MAX = 500_000;

    private final BrukerRepository brukerRepository;
    private final AktoerService aktoerService;

    @Autowired
    public OppfolgingenhetController(BrukerRepository brukerRepository, AktoerService aktoerService) {
        this.brukerRepository = brukerRepository;
        this.aktoerService = aktoerService;
    }

    @GetMapping
    public OppfolgingEnhetPageDTO getOppfolgingEnhet(@DefaultValue("1") @QueryParam("page_number") int pageNumber, @DefaultValue("10") @QueryParam("page_size") int pageSize) {

        autoriserBruker();

        Integer totalNumberOfUsers = brukerRepository.hentAntallBrukereUnderOppfolging().orElseThrow(() -> new WebApplicationException(503));
        long totalNumberOfPages = new BigDecimal(totalNumberOfUsers).divide(new BigDecimal(pageSize), RoundingMode.UP).longValue();

        validatePageSize(pageSize);
        validatePageNumber(pageNumber, totalNumberOfPages);

        List<OppfolgingEnhetDTO> brukereMedOppfolgingsEnhet = brukerRepository.hentBrukereUnderOppfolging(pageNumber, pageSize);

        brukereMedOppfolgingsEnhet.forEach(bruker -> {
            if (bruker.getAktorId() == null) {
                Optional<AktoerId> maybeAktoerId = hentAktoerIdFraAktoerService(bruker);
                if (maybeAktoerId.isPresent()) {
                    bruker.setAktorId(maybeAktoerId.get().toString());
                } else {
                    log.warn("Fant ikke aktørId for bruker med personId {}", bruker.getPersonId());
                }
            }
        });

        return OppfolgingEnhetPageDTO.builder()
                .page_number(pageNumber)
                .page_number_total(totalNumberOfPages)
                .number_of_users(brukereMedOppfolgingsEnhet.size())
                .users(brukereMedOppfolgingsEnhet)
                .build();
    }

    private Optional<AktoerId> hentAktoerIdFraAktoerService(OppfolgingEnhetDTO bruker) {
        return aktoerService.hentAktoeridFraFnr(Fnr.of(bruker.getFnr())).toJavaOptional();
    }

    private void autoriserBruker() {
        IdentType identType = SubjectHandler.getIdentType().orElseThrow(NotFoundException::new);
        String ident = SubjectHandler.getIdent().orElseThrow(NotFoundException::new);

        if (ugyldigIdent(identType, ident)) {
            throw new NotFoundException();
        }
    }

    static boolean ugyldigIdent(IdentType identType, String ident) {
        List<IdentType> internBrukere = Arrays.asList(InternBruker, Systemressurs);
        if (!internBrukere.contains(identType) || !"srvveilarboppfolging".equals(ident)) {
            log.warn("Ident med navn {} og type {} er ugyldig", ident, identType);
            return true;
        } else {
            return false;
        }
    }

    static void validatePageNumber(int pageNumber, long pagesTotal) {

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
