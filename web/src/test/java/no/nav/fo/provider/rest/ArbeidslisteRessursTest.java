package no.nav.fo.provider.rest;

import com.squareup.okhttp.Response;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.testutil.LocalIntegrationTest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static no.nav.fo.database.ArbeidslisteRepository.ARBEIDSLISTE;
import static no.nav.fo.database.BrukerRepository.BRUKERDATA;
import static no.nav.fo.database.BrukerRepository.OPPFOLGINGSBRUKER;
import static no.nav.fo.mock.AktoerServiceMock.*;
import static no.nav.fo.mock.EnhetMock.NAV_SANDE_ID;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.junit.Assert.*;

public class ArbeidslisteRessursTest extends LocalIntegrationTest {

    private static final JdbcTemplate DB = new JdbcTemplate(ds);

    private static final String TEST_VEILEDERID = "testident";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DB.execute("truncate table " + BRUKERDATA);
        DB.execute("truncate table " + OPPFOLGINGSBRUKER);
        DB.execute("truncate table " + ARBEIDSLISTE);
    }

    @Test
    public void skalOppretteOgSletteListe() throws Exception {
        skalOppretteArbeidsliste();
        String path = "/tjenester/arbeidsliste/delete";
        assertFalse(DB.queryForList("select * from ARBEIDSLISTE").isEmpty());

        List<ArbeidslisteRequest> arbeidslisteRequests = new ArrayList<>();
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR));
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR_2));
        JSONArray json = new JSONArray(arbeidslisteRequests);

        Response response = post(path, json.toString());

        assertEquals(200, response.code());
        assertTrue(DB.queryForList("select * from ARBEIDSLISTE").isEmpty());
    }

    @Test
    public void ugyldigFnrSkalGiBadRequest() {
        String path = "/tjenester/arbeidsliste/delete";
        JSONArray json = new JSONArray(Arrays.asList(FNR, "UGYLDIG_FNR"));
        Response response = post(path, json.toString());

        assertEquals(400, response.code());
    }

    @Test
    public void responseSkalInneholdeListeMedFnr() throws Exception {
        skalOppretteArbeidsliste();
        String path = "/tjenester/arbeidsliste/delete";
        List<ArbeidslisteRequest> arbeidslisteRequests = new ArrayList<>();
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR));
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR_2));

        JSONArray json = new JSONArray(arbeidslisteRequests);
        Response response = post(path, json.toString());

        JSONObject responseJSON = new JSONObject(response.body().string());

        assertTrue(responseJSON.get("data").toString().contains(FNR));
        assertTrue(responseJSON.get("data").toString().contains(FNR_2));

    }

    @Test
    public void skalOppretteArbeidsliste() throws Exception {
        insertSuccessfulBrukere();
        String path = "/tjenester/arbeidsliste/";

        JSONObject bruker1 = new JSONObject()
                .put("fnr", FNR)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        JSONObject bruker2 = new JSONObject()
                .put("fnr", FNR_2)
                .put("kommentar", "Dette er en kommentar2")
                .put("frist", "2017-10-10T00:00:00Z");

        JSONArray json = new JSONArray(Arrays.asList(bruker1, bruker2));

        Response response = post(path, json.toString());
        assertEquals(201, response.code());

    }

    @Test
    public void skalIkkeHatilgang() throws Exception {
        insertSuccessfulBrukere();
        insertUnauthorizedBruker();
        String path = "/tjenester/arbeidsliste/";

        JSONObject bruker = new JSONObject()
                .put("fnr", FNR_UNAUTHORIZED)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        JSONObject bruker2 = new JSONObject()
                .put("fnr", FNR_2)
                .put("kommentar", "Dette er en kommentar2")
                .put("frist", "2017-10-10T00:00:00Z");

        JSONArray json = new JSONArray(Arrays.asList(bruker, bruker2));

        Response response = post(path, json.toString());
        assertEquals(403, response.code());
    }

    @Test
    public void skalReturnereUtcStreng() throws Exception {
        insertSuccessfulBrukere();
        String path = "/tjenester/arbeidsliste/" + FNR;

        String expectedUtcString = "2017-10-10T00:00:00Z";
        JSONObject json = new JSONObject()
                .put("kommentar", "Dette er en kommentar")
                .put("frist", expectedUtcString);

        int putStatus = post(path, json.toString()).code();
        assertEquals(201, putStatus);

        Response response = get(path);
        JSONObject body = new JSONObject(response.body().string());
        String actual = body.getString("frist");
        assertEquals(expectedUtcString, actual);

    }

    @Test
    public void skalReturnereOppfolgendeVeilederFlagg() throws Exception {
        insertSuccessfulBrukere();
        String path = "/tjenester/arbeidsliste/" + FNR;

        String expectedUtcString = "2017-10-10T00:00:00Z";
        JSONObject json = new JSONObject()
                .put("kommentar", "Dette er en kommentar")
                .put("frist", expectedUtcString);

        int putStatus = post(path, json.toString()).code();
        assertEquals(201, putStatus);

        Response response = get(path);
        JSONObject body = new JSONObject(response.body().string());
        boolean isOppfolgendeVeileder = body.getBoolean("isOppfolgendeVeileder");
        assertTrue(isOppfolgendeVeileder);
    }


    @Test
    public void skalReturnereNoContentVedUthenting() throws Exception {
        insertSuccessfulBrukere();
        int actual = get("/tjenester/arbeidsliste/" + FNR).code();
        int expected = 204;
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
        insertSuccessfulBrukere();
        String path = "/tjenester/arbeidsliste/" + FNR;
        JSONObject json = new JSONObject()
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int putStatus1 = post(path, json.toString()).code();
        assertEquals(201, putStatus1);
        int putStatus2 = post(path, json.toString()).code();
        assertEquals(201, putStatus2);
    }

    @Test
    public void skalHaTilgangsKontroll() throws Exception {
        insertUnauthorizedBruker();
        String path = "/tjenester/arbeidsliste/" + FNR_UNAUTHORIZED;
        JSONObject json = new JSONObject()
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

    private static void insertSuccessfulBrukere() {
        int result = insert(DB, BRUKERDATA)
                .value("PERSONID", PERSON_ID)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", AKTOER_ID)
                .execute();

        assertTrue(result > 0);

        int result2 = insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", PERSON_ID)
                .value("FODSELSNR", FNR)
                .value("NAV_KONTOR", NAV_SANDE_ID)
                .value("FORNAVN", "REODOR")
                .value("ETTERNAVN", "FELGEN")
                .value("RETTIGHETSGRUPPEKODE", "AAP")
                .value("FORMIDLINGSGRUPPEKODE", "ARBS")
                .value("KVALIFISERINGSGRUPPEKODE", "VARIG")
                .value("RETTIGHETSGRUPPEKODE", "IYT")
                .value("SPERRET_ANSATT", "N")
                .value("ER_DOED", "N")
                .execute();

        assertTrue(result2 > 0);

        int result3 = insert(DB, BRUKERDATA)
                .value("PERSONID", PERSON_ID_2)
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", AKTOER_ID_2)
                .execute();

        assertTrue(result3 > 0);

        int result4 = insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", PERSON_ID_2)
                .value("FODSELSNR", FNR_2)
                .value("NAV_KONTOR", NAV_SANDE_ID)
                .value("FORNAVN", "REODOR")
                .value("ETTERNAVN", "FELGEN")
                .value("RETTIGHETSGRUPPEKODE", "AAP")
                .value("FORMIDLINGSGRUPPEKODE", "ARBS")
                .value("KVALIFISERINGSGRUPPEKODE", "VARIG")
                .value("RETTIGHETSGRUPPEKODE", "IYT")
                .value("SPERRET_ANSATT", "N")
                .value("ER_DOED", "N")
                .execute();

        assertTrue(result4 > 0);


    }

    private static void insertUnauthorizedBruker() {
        int result = insert(DB, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", PERSON_ID_UNAUTHORIZED)
                .value("FODSELSNR", FNR_UNAUTHORIZED)
                .value("NAV_KONTOR", "XOXOXO")
                .execute();

        assertTrue(result > 0);

        int result2 = insert(DB, BRUKERDATA)
                .value("PERSONID", PERSON_ID_UNAUTHORIZED)
                .value("VEILEDERIDENT", "X22222")
                .value("AKTOERID", AKTOER_ID_UNAUTHORIZED)
                .execute();

        assertTrue(result2 > 0);
    }
}
