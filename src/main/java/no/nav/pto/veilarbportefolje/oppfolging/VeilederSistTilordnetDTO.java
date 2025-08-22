package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

import java.time.ZonedDateTime;


@Data
public class VeilederSistTilordnetDTO {
    AktorId aktorId;
    VeilederId veilederId;
    ZonedDateTime tilordnet;

    @JsonCreator
    public VeilederSistTilordnetDTO(
            @JsonProperty("aktorId") final String aktorId,
            @JsonProperty("veilederId") final String veilederId,
            @JsonProperty("tilordnet") final ZonedDateTime tilordnet
    ) {
        this.aktorId = new AktorId(aktorId);
        this.veilederId = new VeilederId(veilederId);
        this.tilordnet = tilordnet;
    }

    public VeilederSistTilordnetDTO(AktorId aktorId, VeilederId veilederId, ZonedDateTime tilordnet) {
        this.aktorId = aktorId;
        this.veilederId = veilederId;
        this.tilordnet = tilordnet;
    }
}
