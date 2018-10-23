package no.nav.fo.veilarbportefolje.provider.rest;

import com.squareup.okhttp.Response;
import no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.veilarbportefolje.testutil.ComponentTest;
import no.nav.fo.veilarbportefolje.util.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.fo.veilarbportefolje.database.ArbeidslisteRepository.ARBEIDSLISTE;
import static no.nav.fo.veilarbportefolje.mock.AktoerServiceMock.*;
import static no.nav.fo.veilarbportefolje.mock.EnhetMock.NAV_SANDE_ID;
import static no.nav.fo.veilarbportefolje.util.sql.SqlUtils.insert;
import static org.junit.Assert.*;

public class ArbeidslisteRessursTest extends ComponentTest {

    private static final JdbcTemplate DB = new JdbcTemplate(ds);

    private static final String TEST_VEILEDERID = "testident";
    public static final String UNAUTHORIZED_NAV_KONTOR = "XOXOXO";

    @Before
    public void setUp() {
        DB.execute("truncate table " + "OPPFOLGING_DATA");
        DB.execute("truncate table " + "OPPFOLGINGSBRUKER");
        DB.execute("truncate table " + ARBEIDSLISTE);
    }

    @Test
    public void skalOppretteOgSletteListe() throws Exception {
        skalOppretteArbeidsliste();
        String path = "/api/arbeidsliste/delete";
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
        String path = "/api/arbeidsliste/delete";
        JSONArray json = new JSONArray(Arrays.asList(FNR, "UGYLDIG_FNR"));
        Response response = post(path, json.toString());

        assertEquals(400, response.code());
    }

    @Test
    public void responseSkalInneholdeListeMedFnr() throws Exception {
        skalOppretteArbeidsliste();
        String path = "/api/arbeidsliste/delete";
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
        long now = Instant.now().toEpochMilli();
        long later = now + 100000000;
        String time = DateUtils.toIsoUTC(new Timestamp(later).toLocalDateTime());
        String path = "/api/arbeidsliste/";

        JSONObject bruker1 = new JSONObject()
                .put("fnr", FNR)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", time);

        JSONObject bruker2 = new JSONObject()
                .put("fnr", FNR_2)
                .put("kommentar", "Dette er en kommentar2")
                .put("frist", time);

        JSONArray json = new JSONArray(Arrays.asList(bruker1, bruker2));

        Response response = post(path, json.toString());
        assertEquals(201, response.code());

    }

    @Test
    public void skalIkkeHatilgang() throws Exception {
        insertSuccessfulBrukere();
        insertUnauthorizedBruker();
        String path = "/api/arbeidsliste/";

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
        String path = "/api/arbeidsliste/" + FNR;

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
        String path = "/api/arbeidsliste/" + FNR;

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
    public void skalReturnereTomArbeidslisteUthenting() throws Exception {
        int expected = 200;
        insertSuccessfulBrukere();
        Response resp = get("/api/arbeidsliste/" + FNR);
        JSONObject json = new JSONObject(resp.body().string());


        assertEquals(json.get("arbeidslisteAktiv").toString(), "null");
        assertEquals(expected, resp.code());
    }

    @Test
    public void skalReturnereTomArbeidslisteSelvOmVeilederOgEllerEnhetMangler() throws Exception {
        int actual = get("/api/arbeidsliste/" + FNR).code();
        int expected = 200;
        assertEquals(expected, actual);
    }

    @Test
    public void skalIkkeGodtaUgyldigFnr() throws Exception {
        int expected = 400;
        int actual = get("/api/arbeidsliste/123").code();
        assertEquals(expected, actual);
    }

    @Test
    public void skalKunneOppretteSammeArbeidslisteToGanger() throws Exception {
        insertSuccessfulBrukere();
        String path = "/api/arbeidsliste/" + FNR;
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
        String path = "/api/arbeidsliste/" + FNR_UNAUTHORIZED;
        JSONObject json = new JSONObject()
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "2017-10-10T00:00:00Z");

        int expected = 403;
        int expectedGet = 200;

        int actualPut = put(path, json.toString()).code();
        assertEquals(expected, actualPut);

        int actualDelete = delete(path).code();
        assertEquals(expected, actualDelete);

        int actualGet = get(path).code();
        assertEquals(expectedGet, actualGet);
        assertEquals(new JSONObject(get(path).body().string()).getBoolean("harVeilederTilgang"), false);

        int actualPost = post(path, json.toString()).code();
        assertEquals(expected, actualPost);
    }

    @Test
    public void datoFeltSkalVaereValgfritt() throws Exception {
        insertSuccessfulBrukere();
        String path = "/api/arbeidsliste/";

        JSONObject utenDato = new JSONObject()
                .put("fnr", FNR_2)
                .put("kommentar", "Dette er en kommentar");

        JSONArray json = new JSONArray(singletonList(utenDato));

        Response response = post(path, json.toString());
        assertEquals(201, response.code());
    }

    @Test
    public void datoSkalVaereFramITid() throws Exception {
        insertSuccessfulBrukere();
        String path = "/api/arbeidsliste/";

        JSONObject utenDato = new JSONObject()
                .put("fnr", FNR_2)
                .put("kommentar", "Dette er en kommentar")
                .put("frist", "1985-07-23T00:00:00Z");

        JSONArray json = new JSONArray(singletonList(utenDato));

        Response response = post(path, json.toString());
        assertEquals(400, response.code());
    }

    @Test
    public void datoSkalKunneSettesTilbakeITidVedRedigering() throws Exception {
        insertSuccessfulBrukere();
        String path = "/api/arbeidsliste/";

        // opprett
        JSONObject utenDato = new JSONObject()
                .put("fnr", FNR_2)
                .put("frist", "2100-07-23T00:00:00Z")
                .put("kommentar", "Dette er en kommentar");
        JSONArray json = new JSONArray(singletonList(utenDato));
        Response opprettResponse = post(path, json.toString());
        assertEquals(201, opprettResponse.code());

        // oppdater
        utenDato.put("frist", "1985-07-23T00:00:00Z");
        Response oppdaterResponse = put(path + FNR_2, utenDato.toString());
        assertEquals(200, oppdaterResponse.code());
    }

    private static void insertSuccessfulBrukere() {
        int result = insert(DB, "OPPFOLGING_DATA")
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", AKTOER_ID)
                .execute();

        assertTrue(result > 0);

        int result2 = insert(DB, "OPPFOLGINGSBRUKER")
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

        int result3 = insert(DB, "OPPFOLGING_DATA")
                .value("VEILEDERIDENT", TEST_VEILEDERID)
                .value("AKTOERID", AKTOER_ID_2)
                .execute();

        assertTrue(result3 > 0);

        int result4 = insert(DB, "OPPFOLGINGSBRUKER")
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
        int result = insert(DB, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", PERSON_ID_UNAUTHORIZED)
                .value("FODSELSNR", FNR_UNAUTHORIZED)
                .value("NAV_KONTOR", UNAUTHORIZED_NAV_KONTOR)
                .execute();

        assertTrue(result > 0);

        int result2 = insert(DB, "OPPFOLGING_DATA")
                .value("VEILEDERIDENT", "X22222")
                .value("AKTOERID", AKTOER_ID_UNAUTHORIZED)
                .execute();

        assertTrue(result2 > 0);
    }
}
