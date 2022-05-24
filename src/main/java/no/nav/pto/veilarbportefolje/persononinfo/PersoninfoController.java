package no.nav.pto.veilarbportefolje.persononinfo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personinfo")
public class PersoninfoController {

    @GetMapping("/{fnr}")
    @ResponseStatus(HttpStatus.GONE)
    public void hentPersoninfo(@PathVariable("fnr") String fnr) {
    }

}
