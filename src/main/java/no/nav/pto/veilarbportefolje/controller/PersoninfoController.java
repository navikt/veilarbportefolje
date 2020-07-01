package no.nav.pto.veilarbportefolje.controller;

import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.database.PersonRepository;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping("/api/personinfo")
@Produces(APPLICATION_JSON)
public class PersoninfoController {
    private final AuthService authService;
    private final PersonRepository personRepository;

    @Autowired
    public PersoninfoController(AuthService authService, PersonRepository personRepository) {
        this.authService = authService;
        this.personRepository = personRepository;
    }

    @GetMapping("/{fnr}")
    public Personinfo hentPersoninfo(@PathVariable("fnr") String fnr) {
        authService.tilgangTilBruker(fnr);
        return personRepository.hentPersoninfoForFnr(Fnr.of(fnr))
                .orElseThrow(() -> new NotFoundException("Kunne ikke finne personinfo for: " + fnr));
    }
}
