package no.nav.fo.mock;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Testbruker {
    private String fnr;
    private String person_id;
    private String aktoer_id;
}
