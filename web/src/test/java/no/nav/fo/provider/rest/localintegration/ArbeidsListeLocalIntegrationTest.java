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

    private static final String TEST_VEILEDERID = "testident";
    private static final String TEST_FNR = "01010101010";
    private static final String TEST_PERSONID = "00000";
    private static final JdbcTemplate db = new JdbcTemplate(ds);
    private static final String NOT_FOUND_FNR = "00000000000";
    private static final String NOT_FOUND_PERSONID = "11111";

    @BeforeClass
    public static void setUpDb() throws Exception {
        insertSuccessfulBruker();
        insertNotFoundBruker();
    }

    @AfterClass
    public static void tearDownDb() throws Exception {
        db.execute("truncate table " + BRUKERDATA);
        db.execute("truncate table " + OPPFOLGINGSBRUKER);
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

    private static void insertSuccessfulBruker() {
        insert(db, BRUKERDATA)
                .value("PERSONID", TEST_PERSONID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", createTestId(TEST_FNR))
                .execute();

        insert(db, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", TEST_PERSONID)
                .value("FODSELSNR", TEST_FNR)
                .value("NAV_KONTOR", getTestEnhetId())
                .execute();
    }

    private static void insertNotFoundBruker() {
        insert(db, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", NOT_FOUND_PERSONID)
                .value("FODSELSNR", NOT_FOUND_FNR)
                .value("NAV_KONTOR", getTestEnhetId())
                .execute();

        insert(db, BRUKERDATA)
                .value("PERSONID", NOT_FOUND_PERSONID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", createTestId(NOT_FOUND_FNR))
                .execute();
    }
}
