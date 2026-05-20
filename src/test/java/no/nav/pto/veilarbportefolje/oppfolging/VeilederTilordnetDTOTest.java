package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class VeilederTilordnetDTOTest {

    @Test
    public void test_deserialization_when_veilederid_is_null() {
        String json = "{\"aktorId\": \"0000\",\"veilederId\": null}";

        VeilederTilordnetDTO veilederTilordnetDTO = JsonUtils.fromJson(json, VeilederTilordnetDTO.class);
        Assert.assertEquals(veilederTilordnetDTO.getAktorId().toString(), "0000");
        Assert.assertNull(veilederTilordnetDTO.getVeilederId().getValue());
    }

}
