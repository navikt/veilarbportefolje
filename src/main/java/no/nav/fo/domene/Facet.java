package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Facet {
    private String value;
    private long count;
}
