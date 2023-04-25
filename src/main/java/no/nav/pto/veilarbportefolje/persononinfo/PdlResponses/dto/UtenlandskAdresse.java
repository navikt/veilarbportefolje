package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UtenlandskAdresse {
    private String adressenavnNummer;
    private String bygningEtasjeLeilighet;
    private String postboksNummerNavn;
    private String postkode;
    private String bySted;
    private String regionDistriktOmraade;
    private String landkode;
}
