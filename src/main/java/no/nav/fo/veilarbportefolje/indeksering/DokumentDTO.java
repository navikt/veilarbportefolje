package no.nav.fo.veilarbportefolje.indeksering;

import lombok.Value;
import no.nav.json.JsonUtils;

@Value
public class DokumentDTO {
    String id;
    String json;

    public DokumentDTO(BrukerDTO bruker) {
        this.id = bruker.fnr;
        this.json = JsonUtils.toJson(bruker);
    }
}
