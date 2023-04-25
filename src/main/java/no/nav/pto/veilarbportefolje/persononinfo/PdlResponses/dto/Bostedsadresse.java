package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bostedsadresse {
    private Vegadresse vegadresse;
    private Matrikkeladresse matrikkeladresse;
    private UtenlandskAdresse utenlandskAdresse;
    private UkjentBosted ukjentBosted;
    private Metadata metadata;
}

