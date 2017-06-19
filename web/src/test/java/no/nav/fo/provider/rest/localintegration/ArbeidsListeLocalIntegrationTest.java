package no.nav.fo.provider.rest.localintegration;

import no.nav.fo.testutil.LocalIntegrationTest;
import org.json.JSONObject;
import org.junit.Test;

import static no.nav.fo.mock.AktoerServiceMock.getFailingFnr;
import static org.junit.Assert.assertEquals;

public class ArbeidsListeLocalIntegrationTest extends LocalIntegrationTest {

    @Test
    public void skalOppretteOppdatereHenteOgSlette() throws Exception {
        String path = "/tjenester/arbeidsliste/01010101010";

        JSONObject json = new JSONObject()
                .put("veilederId", "X11111")
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int putStatus = put(path, json.toString()).code();
        assertEquals(201, putStatus);

        String expectedVeilederId = "X22222";
        json.put("veilederId", expectedVeilederId);
        int postStatus = post(path, json.toString()).code();
        assertEquals(200, postStatus);

        String jsonAfterUpdate = get(path).body().string();

        Object actualVeilederId = new JSONObject(jsonAfterUpdate).get("veilederId");
        assertEquals(expectedVeilederId, actualVeilederId);

        int deleteStatus = delete(path).code();
        assertEquals(200, deleteStatus);
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

    @Test
    public void skalReturnereBadGateway() throws Exception {
        int expected = 502;
        int actual = get("/tjenester/arbeidsliste/" + getFailingFnr()).code();
        assertEquals(expected, actual);
    }

    @Test
    public void skalIkkeGodtaUgyldigFnr() throws Exception {
        int expected = 400;
        int actual = get("/tjenester/arbeidsliste/123").code();
        assertEquals(expected, actual);
    }

    @Test
    public void skalKunneOppretteSammeArbeidslisteToGanger() throws Exception {
        String path = "/tjenester/arbeidsliste/01010101010";
        JSONObject json = new JSONObject()
                .put("veilederId", "X11111")
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int putStatus1 = put(path, json.toString()).code();
        assertEquals(201, putStatus1);
        int putStatus2 = put(path, json.toString()).code();
        assertEquals(201, putStatus2);
    }

}
