package no.nav.pto.veilarbportefolje.pdldata;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.Fnr;

/*
    NB: Klassen inneholder ikke alle data feleter returnert av veialrbperson.
 */
@Data
@Accessors(chain = true)
public class PersonData {
    String fornavn;
    String mellomnavn;
    String etternavn;
    String sammensattNavn;
    Fnr fodselsnummer;
    String fodselsdato;
    String kjonn;
    String dodsdato;

    String diskresjonskode;
    String sikkerhetstiltak;

    boolean egenAnsatt; // denne blir hentet fra veialrbportefolje

}