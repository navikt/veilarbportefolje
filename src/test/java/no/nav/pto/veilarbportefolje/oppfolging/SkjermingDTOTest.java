package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SkjermingDTOTest {

    @Test
    public void testDeserialization() {
        String json = """
                {"skjermetFra":[2022,3,23,14,53,54],"skjermetTil":null}""";

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SkjermingDTO skjermingDTO = objectMapper.readValue(json, SkjermingDTO.class);

            Assertions.assertEquals(skjermingDTO.getSkjermetFra().length, 6);
            Assertions.assertNull(skjermingDTO.getSkjermetTil());
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}