package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Builder;
import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.util.Result;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.Optional;

import static java.time.OffsetDateTime.parse;

@Value
@Builder
public class OppfolgingStatus {
    AktoerId aktoerId;
    Optional<VeilederId> veilederId;
    boolean oppfolging;
    boolean nyForVeileder;
    boolean manuell;
    OffsetDateTime endretTimestamp;
    OffsetDateTime startDato;

    public static OppfolgingStatus fromJson(String payload) {


        JSONObject json = new JSONObject(payload);
        Optional<VeilederId> maybeVeileder = Result.of(() -> json.getString("veileder"))
                .map(
                        t -> Optional.empty(),
                        v -> Optional.of(VeilederId.of(v))
                );

        return OppfolgingStatus.builder()
                .aktoerId(AktoerId.of(json.getString("aktoerid")))
                .veilederId(maybeVeileder)
                .oppfolging(json.getBoolean("oppfolging"))
                .nyForVeileder(json.getBoolean("nyForVeileder"))
                .manuell(json.getBoolean("manuell"))
                .endretTimestamp(parse(json.getString("endretTimestamp")))
                .startDato(parse(json.getString("startDato")))
                .build();
    }
}


