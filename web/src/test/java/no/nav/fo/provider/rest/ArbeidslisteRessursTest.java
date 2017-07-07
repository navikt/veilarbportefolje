package no.nav.fo.provider.rest;

import com.squareup.okhttp.Response;
import no.nav.fo.testutil.LocalIntegrationTest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.fo.database.ArbeidslisteRepository.ARBEIDSLISTE;
import static no.nav.fo.database.BrukerRepository.BRUKERDATA;
import static no.nav.fo.database.BrukerRepository.OPPFOLGINGSBRUKER;
import static no.nav.fo.mock.AktoerServiceMock.*;
import static no.nav.fo.mock.EnhetMock.NAV_SANDE_ID;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArbeidslisteRessursTest extends LocalIntegrationTest {

    private static final JdbcTemplate DB = new JdbcTemplate(ds);

    private static final String TEST_VEILEDERID = "testident";

    private static final String NOT_FOUND_PERSONID = "11111";

    private static final String UNAUTHORIZED_FNR = "11111111111";
    private static final String UNAUTHORIZED_PERSONID = "1111";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DB.execute("truncate table " + BRUKERDATA);
        DB.execute("truncate table " + OPPFOLGINGSBRUKER);
        DB.execute("truncate table " + ARBEIDSLISTE);
    }

    @Test
    public void skalOppretteOppdatereHenteOgSlette() throws Exception {
        insertSuccessfulBruker();
        String path = "/tjenester/arbeidsliste/" + FNR;

        JSONObject json = new JSONObject()
                .put("veilederId", TEST_VEILEDERID)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int putStatus = put(path, json.toString()).code();
        assertEquals(201, putStatus);

        String expectedKommentar = "Dette er en NY kommentar";
        json.put("kommentar", expectedKommentar);
        int postStatus = post(path, json.toString()).code();
        assertEquals(200, postStatus);

        Response getResponse = get(path);
        assertEquals(200, getResponse.code());

        String jsonAfterUpdate = getResponse.body().string();
        String actualKommentar = (String) new JSONObject(jsonAfterUpdate).get("kommentar");
        assertEquals(expectedKommentar, actualKommentar);

        int deleteStatus = delete(path).code();
        assertEquals(200, deleteStatus);
    }

    @Test
    public void skalReturnereUtcStreng() throws Exception {
        insertSuccessfulBruker();
        String path = "/tjenester/arbeidsliste/" + FNR;

        String expectedUtcString = "2017-10-10T00:00:00Z";
        JSONObject json = new JSONObject()
                .put("veilederId", TEST_VEILEDERID)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", expectedUtcString);

        int putStatus = put(path, json.toString()).code();
        assertEquals(201, putStatus);

        Response response = get(path);
        JSONObject body = new JSONObject(response.body().string());
        String actual = body.getString("frist");
        assertEquals(expectedUtcString, actual);

    }

    @Test
    public void skalReturnereOppfolgendeVeilederFlagg() throws Exception {
        insertSuccessfulBruker();
        String path = "/tjenester/arbeidsliste/" + FNR;

        String expectedUtcString = "2017-10-10T00:00:00Z";
        JSONObject json = new JSONObject()
                .put("veilederId", TEST_VEILEDERID)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", expectedUtcString);

        int putStatus = put(path, json.toString()).code();
        assertEquals(201, putStatus);

        Response response = get(path);
        JSONObject body = new JSONObject(response.body().string());
        boolean isOppfolgendeVeileder = body.getBoolean("isOppfolgendeVeileder");
        assertTrue(isOppfolgendeVeileder);
    }


    @Test
    public void skalReturnereNotFoundVedUthenting() throws Exception {
        insertSuccessfulBruker();
        int actual = get("/tjenester/arbeidsliste/" + FNR).code();
        int expected = 404;
        assertEquals(expected, actual);
    }

    @Test
    public void skalReturnereNotFoundVedSletting() throws Exception {
        insertSuccessfulBruker();
        int actual = delete("/tjenester/arbeidsliste/" + FNR).code();
        int expected = 404;
        assertEquals(expected, actual);
    }

    @Test
    public void skalReturnereBadGateway() throws Exception {
        insertUnauthorizedBruker();
        int expected = 502;
        int actual = get("/tjenester/arbeidsliste/" + FNR_FAIL).code();
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
        insertSuccessfulBruker();
        String path = "/tjenester/arbeidsliste/" + FNR;
        JSONObject json = new JSONObject()
                .put("veilederId", TEST_VEILEDERID)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int putStatus1 = put(path, json.toString()).code();
        assertEquals(201, putStatus1);
        int putStatus2 = put(path, json.toString()).code();
        assertEquals(201, putStatus2);
    }

    @Test
    public void skalHaTilgangsKontroll() throws Exception {
        insertUnauthorizedBruker();
        String path = "/tjenester/arbeidsliste/" + UNAUTHORIZED_FNR;
        JSONObject json = new JSONObject()
                .put("veilederId", TEST_VEILEDERID)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int expected = 403;

        int actualPut = put(path, json.toString()).code();
        assertEquals(expected, actualPut);

        int actualDelete = delete(path).code();
        assertEquals(expected, actualDelete);

        int actualGet = get(path).code();
        assertEquals(expected, actualGet);

        int actualPost = post(path, json.toString()).code();
        assertEquals(expected, actualPost);
    }

    private static void insertSuccessfulBruker() {
        insert(DB, BRUKERDATA)
                .value("PERSONID", PERSON_ID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", AKTOER_ID)
                .execute();

        insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", PERSON_ID)
                .value("FODSELSNR", FNR)
                .value("NAV_KONTOR", NAV_SANDE_ID)
                .value("FORNAVN", "TEST")
                .value("ETTERNAVN", "ETTERNAVN")
                .value("RETTIGHETSGRUPPEKODE", "AAP")
                .value("FORMIDLINGSGRUPPEKODE", "ARBS")
                .value("KVALIFISERINGSGRUPPEKODE", "VARIG")
                .value("RETTIGHETSGRUPPEKODE", "IYT")
                .value("SPERRET_ANSATT", "N")
                .value("ER_DOED", "N")
                .execute();

    }

    private static void insertNotFoundBruker() {
        insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", NOT_FOUND_PERSONID)
                .value("FODSELSNR", FNR_FAIL)
                .value("NAV_KONTOR", NAV_SANDE_ID)
                .execute();

        insert(DB, BRUKERDATA)
                .value("PERSONID", NOT_FOUND_PERSONID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", "NOPE")
                .execute();
    }

    private static void insertUnauthorizedBruker() {
        insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", UNAUTHORIZED_PERSONID)
                .value("FODSELSNR", UNAUTHORIZED_FNR)
                .value("NAV_KONTOR", "XOXOXO")
                .execute();

        insert(DB, BRUKERDATA)
                .value("PERSONID", UNAUTHORIZED_PERSONID)
                .value("VEILEDERIDENT", "X22222")
                .value("AKTOERID", UNAUTHORIZED_FNR)
                .execute();
    }

}
