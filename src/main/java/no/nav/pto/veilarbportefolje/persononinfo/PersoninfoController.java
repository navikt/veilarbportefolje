package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personinfo")
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
        return personRepository.hentPersoninfoForFnr(Fnr.of(fnr)).orElseThrow();
    }

}
