package no.nav.fo.smoketest.domene;


import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PortefoljeEnhet {
    String enhetId;
    String navn;
}
