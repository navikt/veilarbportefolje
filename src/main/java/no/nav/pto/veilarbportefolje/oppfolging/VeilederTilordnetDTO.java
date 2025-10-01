package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

import java.time.ZonedDateTime;

@Data
public class VeilederTilordnetDTO {
    AktorId aktorId;
    VeilederId veilederId;
    ZonedDateTime tilordnetTidspunkt;

    @JsonCreator
    public VeilederTilordnetDTO(
            @JsonProperty("aktorId") final String aktorId,
            @JsonProperty("veilederId") final String veilederId,
            @JsonProperty("tilordnet") final ZonedDateTime tilordnetTidspunkt
    ) {
        this.aktorId = new AktorId(aktorId);
        this.veilederId = new VeilederId(veilederId);
        this.tilordnetTidspunkt = tilordnetTidspunkt;
    }

    public VeilederTilordnetDTO(AktorId aktorId, VeilederId veilederId, ZonedDateTime tilordnetTidspunkt) {
        this.aktorId = aktorId;
        this.veilederId = veilederId;
        this.tilordnetTidspunkt = tilordnetTidspunkt;
    }
}
