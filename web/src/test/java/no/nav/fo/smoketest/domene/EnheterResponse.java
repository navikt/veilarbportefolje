package no.nav.fo.smoketest.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.domene.Veileder;

import java.util.List;

@Data
@Accessors(chain = true)
public class EnheterResponse {
    List<PortefoljeEnhet> enhetliste;
    Veileder veileder;
}
