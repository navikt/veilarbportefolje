package no.nav.fo.provider.rest.localintegration;

import com.squareup.okhttp.Response;
import no.nav.fo.testutil.LocalIntegrationTest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArbeidsListeLocalIntegrationTest extends LocalIntegrationTest {

    //language=JSON
    private static final String JSON = "{\n" +
            "  \"veilederId\" : \"X11111\",\n" +
            "  \"kommentar\": \"Dette er en kommentar\",\n" +
            "  \"frist\" : \"2017-10-10T00:00:00Z\"\n" +
            "}";
    private final String PATH = "/tjenester/arbeidsliste/01010101010";

    @Before
    public void setUp() throws Exception {
        delete(PATH);
    }

    @Test
    public void skalHenteUtArbeidsliste() throws Exception {
        putArbeidsliste(PATH);

        int expectedStatus = 200;
        Response response = get(PATH);

        int actualStatus = response.code();
        assertEquals(expectedStatus, actualStatus);
    }

    @Test
    public void skalSletteArbeidsliste() throws Exception {
        putArbeidsliste(PATH);

        int expectedStatus = 200;
        int actualStatus = delete(PATH).code();
        assertEquals(expectedStatus, actualStatus);
    }

    @Test
    public void skalOppdatereArbeidsliste() throws Exception {
        putArbeidsliste(PATH);

        //language=JSON
        String json = "{\n" +
                "  \"veilederId\": \"X22222\",\n" +
                "  \"kommentar\": \"Dette er en NY kommentar\",\n" +
                "  \"frist\": \"2017-10-10T00:00:00Z\"\n" +
                "}";

        int expectedStatus = 200;
        int actualStatus = post(PATH, json).code();
        assertEquals(expectedStatus, actualStatus);

        String jsonAfterUpdate = get(PATH).body().string();

        String expectedVeilederId = "X22222";
        Object actualVeilederId = new JSONObject(jsonAfterUpdate).get("veilederId");
        assertEquals(expectedVeilederId, actualVeilederId);
    }

    @Test
    public void skalReturnereNotFoundVedUthenting() throws Exception {
        int actual = get("/tjenester/arbeidsliste/12345678900").code();
        int expected = 404;
        assertEquals(expected, actual);
    }

    @Test
    public void skalReturnereNotFoundVedSletting() throws Exception {
        int actual = delete("/tjenester/arbeidsliste/12345678900").code();
        int expected = 404;
        assertEquals(expected, actual);
    }

    private void putArbeidsliste(String PATH) {
        int expectedPutStatus = 201;
        int actualPutStatus = put(PATH, JSON).code();
        assertEquals(expectedPutStatus, actualPutStatus);
    }

}
