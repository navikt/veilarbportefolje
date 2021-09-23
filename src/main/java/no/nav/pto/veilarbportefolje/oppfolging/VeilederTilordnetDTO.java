package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

@Data
public class VeilederTilordnetDTO {
    AktorId aktorId;
    VeilederId veilederId;

    @JsonCreator
    public VeilederTilordnetDTO(@JsonProperty("aktorId") final String aktorId, @JsonProperty("veilederId") final String veilederId) {
        this.aktorId = new AktorId(aktorId);
        this.veilederId = new VeilederId(veilederId);
    }

    public VeilederTilordnetDTO(AktorId aktorId, VeilederId veilederId) {
        this.aktorId = aktorId;
        this.veilederId = veilederId;
    }
}
