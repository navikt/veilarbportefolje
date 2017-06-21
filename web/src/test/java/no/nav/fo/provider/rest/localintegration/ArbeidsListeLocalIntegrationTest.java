package no.nav.fo.provider.rest.localintegration;

import com.squareup.okhttp.Response;
import no.nav.fo.testutil.LocalIntegrationTest;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.fo.database.BrukerRepository.BRUKERDATA;
import static no.nav.fo.database.BrukerRepository.OPPFOLGINGSBRUKER;
import static no.nav.fo.mock.AktoerServiceMock.createTestId;
import static no.nav.fo.mock.AktoerServiceMock.getFailingFnr;
import static no.nav.fo.mock.EnhetMock.getTestEnhetId;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.junit.Assert.assertEquals;

public class ArbeidsListeLocalIntegrationTest extends LocalIntegrationTest {

    private static final JdbcTemplate DB = new JdbcTemplate(ds);

    private static final String TEST_VEILEDERID = "testident";
    private static final String TEST_FNR = "01010101010";
    private static final String TEST_PERSONID = "00000";

    private static final String NOT_FOUND_FNR = "00000000000";
    private static final String NOT_FOUND_PERSONID = "11111";

    private static final String UNAUTHORIZED_FNR = "11111111111";
    private static final String UNAUTHORIZED_PERSONID = "1111";

    @BeforeClass
    public static void setUpDb() throws Exception {
        insertSuccessfulBruker();
        insertNotFoundBruker();
        insertUnauthorizedBruker();
    }

    @AfterClass
    public static void tearDownDb() throws Exception {
        DB.execute("truncate table " + BRUKERDATA);
        DB.execute("truncate table " + OPPFOLGINGSBRUKER);
    }

    @Test
    public void skalOppretteOppdatereHenteOgSlette() throws Exception {
        String path = "/tjenester/arbeidsliste/" + TEST_FNR;

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
        Object actualKommentar = new JSONObject(jsonAfterUpdate).get("kommentar");
        assertEquals(expectedKommentar, actualKommentar);

        int deleteStatus = delete(path).code();
        assertEquals(200, deleteStatus);
    }

    @Test
    public void skalReturnereNotFoundVedUthenting() throws Exception {
        int actual = get("/tjenester/arbeidsliste/" + NOT_FOUND_FNR).code();
        int expected = 404;
        assertEquals(expected, actual);
    }

    @Test
    public void skalReturnereNotFoundVedSletting() throws Exception {
        int actual = delete("/tjenester/arbeidsliste/" + NOT_FOUND_FNR).code();
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
        String path = "/tjenester/arbeidsliste/" + TEST_FNR;
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
                .value("PERSONID", TEST_PERSONID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", createTestId(TEST_FNR))
                .execute();

        insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", TEST_PERSONID)
                .value("FODSELSNR", TEST_FNR)
                .value("NAV_KONTOR", getTestEnhetId())
                .execute();
    }

    private static void insertNotFoundBruker() {
        insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", NOT_FOUND_PERSONID)
                .value("FODSELSNR", NOT_FOUND_FNR)
                .value("NAV_KONTOR", getTestEnhetId())
                .execute();

        insert(DB, BRUKERDATA)
                .value("PERSONID", NOT_FOUND_PERSONID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", createTestId(NOT_FOUND_FNR))
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
                .value("AKTOERID", createTestId(UNAUTHORIZED_FNR))
                .execute();
    }

}
