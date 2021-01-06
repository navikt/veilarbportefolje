package no.nav.pto.veilarbportefolje.domene.value;

import no.nav.common.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VeilederIdTest {
    @Test
    void skal_deserialisere_veileder_id_riktig() {
        final VeilederId veilederId = VeilederId.of("Z000000");
        final String json = JsonUtils.toJson(veilederId);
        assertThat(json).isEqualTo("{\"veilederId\":\"Z000000\"}");
    }
}
