package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VeilederSistTilordnetDTOTest {

    @Test
    public void test_deserialization_when_veilederid_og_tilordnet_is_null() {
        String json = "{\"aktorId\": \"0000\",\"veilederId\": null, \"tilordnet\": null}";

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            VeilederSistTilordnetDTO veilederSistTilordnetDTO = objectMapper.readValue(json, VeilederSistTilordnetDTO.class);
            Assertions.assertEquals("0000", veilederSistTilordnetDTO.getAktorId().toString());
            Assertions.assertNull(veilederSistTilordnetDTO.getVeilederId().toString());
            Assertions.assertNull(veilederSistTilordnetDTO.getTilordnet());
        } catch (JsonProcessingException e) {
            Assertions.fail();
        }

    }

    @Test
    public void test_deserialization_when_not_null() throws Exception {
        String json = "{\"aktorId\": \"0000\",\"veilederId\":  \"z1000\", \"tilordnet\":  \"2023-10-01T12:00:00Z\"}";

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        VeilederSistTilordnetDTO dto = objectMapper.readValue(json, VeilederSistTilordnetDTO.class);

        Assertions.assertEquals("0000", dto.getAktorId().toString());
        Assertions.assertEquals("z1000", dto.getVeilederId().toString());
        Assertions.assertEquals("2023-10-01T12:00Z", dto.getTilordnet().toString());
    }

}
