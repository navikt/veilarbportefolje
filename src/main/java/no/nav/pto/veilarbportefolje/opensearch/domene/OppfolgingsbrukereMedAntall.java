package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingsbrukereMedAntall {
    private final int antall;
    private final List<OppfolgingsBruker> brukere;

    public OppfolgingsbrukereMedAntall(int antall, List<OppfolgingsBruker> brukere) {
        this.antall = antall;
        this.brukere = brukere;
    }
}
