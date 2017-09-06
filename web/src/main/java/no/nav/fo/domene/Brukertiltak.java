package no.nav.fo.domene;

import lombok.Value;
import lombok.experimental.Wither;

@Value(staticConstructor = "of")
@Wither
public class Brukertiltak {
    private Fnr fnr;
    private String tiltak;
}
