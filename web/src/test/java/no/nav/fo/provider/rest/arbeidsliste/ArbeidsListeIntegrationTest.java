package no.nav.fo.provider.rest.arbeidsliste;

import no.nav.fo.jetty.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class ArbeidsListeIntegrationTest extends JettyTest {

    @Test
    public void skalReturnere404() throws Exception {
        int actual = get("/tjenester/arbeidsliste/12345678900").getStatus();
        int expected = 404;
        assertEquals(expected, actual);
    }

    @Test
    public void skalOppretteEnArbeidsliste() throws Exception {
        String path = "/tjenester/arbeidsliste/01010101010";
        String json = "{\n" +
                "  \"veilederid\": \"X11111\",\n" +
                "  \"kommentar\": \"Dette er en kommentar\",\n" +
                "  \"frist\": \"2017-10-11T00:00:00Z\"\n" +
                "}";

        int expectedPutStatus = 201;
        int actualPutStatus = put(path, json).getStatus();
        assertEquals(expectedPutStatus, actualPutStatus);

        Response getRespone = get(path);
    }
}