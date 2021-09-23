package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class VeilederTilordnetDTOTest {

    @Test
    public void test_deserialization_when_veilederid_is_null() {
        String json = "{\"aktorId\": \"0000\",\"veilederId\": null}";

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            VeilederTilordnetDTO veilederTilordnetDTO = objectMapper.readValue(json, VeilederTilordnetDTO.class);
            Assert.assertEquals(veilederTilordnetDTO.getAktorId().toString(), "0000");
            Assert.assertNull(veilederTilordnetDTO.getVeilederId().toString());
        } catch (JsonProcessingException e) {
            Assert.fail();
        }

    }

}