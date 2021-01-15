package no.nav.pto.veilarbportefolje.elastic.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Endring {
    private String aktivtetId;
    private String tidspunkt;

    public Endring(){

    }

    @JsonCreator
    public Endring(String tidspunkt){
        this.tidspunkt = tidspunkt;
    }
}
