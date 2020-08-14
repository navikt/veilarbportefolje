package no.nav.pto.veilarbportefolje.krr;

import lombok.Data;

@Data
public class DkifKontaktinfo {
    String personident;
    boolean kanVarsles;
    boolean reservert;
    String epostadresse;
    String mobiltelefonnummer;
}