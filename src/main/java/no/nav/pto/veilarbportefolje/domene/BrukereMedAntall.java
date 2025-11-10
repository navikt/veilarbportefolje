package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell;

import java.util.List;

@Data
@Accessors(chain = true)
public class BrukereMedAntall {
    private final int antall;
    private final List<PortefoljebrukerFrontendModell> brukere;

    public BrukereMedAntall(int antall, List<PortefoljebrukerFrontendModell> brukere) {
        this.antall = antall;
        this.brukere = brukere;
    }
}
