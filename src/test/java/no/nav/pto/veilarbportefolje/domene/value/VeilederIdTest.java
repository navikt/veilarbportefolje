package no.nav.pto.veilarbportefolje.domene.value;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class VeilederIdTest {
    @Test
    public void skal_deserialisere_veileder_id_riktig() {
        final VeilederId veilederId = VeilederId.of("Z000000");
        final String json = JsonUtils.toJson(veilederId);
        assertThat(json).isEqualTo("{\"veilederId\":\"Z000000\"}");
    }
}
