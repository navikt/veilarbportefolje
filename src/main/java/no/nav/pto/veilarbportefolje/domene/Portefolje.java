package no.nav.pto.veilarbportefolje.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Portefolje {
    String enhet;
    int antallTotalt;
    int antallReturnert;
    int fraIndex;
    List<PortefoljebrukerFrontendModell> brukere;
}
