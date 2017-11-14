package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BrukereMedAntall {
    private final int antall;
    private final List<Bruker> brukere;
}
