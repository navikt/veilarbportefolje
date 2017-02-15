package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Portefolje {
    public String enhet;
    public int antallTotalt;
    public int antallReturnert;
    public int fraIndex;
    public List<Bruker> brukere;
}
