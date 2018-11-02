package no.nav.fo.veilarbportefolje.provider.rest;

import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.common.auth.Subject;
import no.nav.fo.veilarbportefolje.config.ComponentTestConfig;
import no.nav.fo.veilarbportefolje.domene.Arbeidsliste;
import no.nav.fo.veilarbportefolje.domene.RestResponse;
import no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste.ArbeidslisteRequest;
import no.nav.fo.veilarbportefolje.util.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.fo.veilarbportefolje.database.ArbeidslisteRepository.ARBEIDSLISTE;
import static no.nav.fo.veilarbportefolje.mock.AktoerServiceMock.*;
import static no.nav.fo.veilarbportefolje.mock.EnhetMock.NAV_SANDE_ID;
import static no.nav.fo.veilarbportefolje.util.sql.SqlUtils.insert;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ComponentTestConfig.class})
public class ArbeidslisteRessursTest {

    @Inject
    private ArbeidsListeRessurs arbeidsListeRessurs;

    @Inject
    private JdbcTemplate DB;

    private static final String TEST_VEILEDERID = "testident";
    public static final String UNAUTHORIZED_NAV_KONTOR = "XOXOXO";

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject(TEST_VEILEDERID, InternBruker, oidcToken("token")));

    @Before
    public void setUp() {
        DB.execute("truncate table " + "OPPFOLGING_DATA");
        DB.execute("truncate table " + "OPPFOLGINGSBRUKER");
        DB.execute("truncate table " + ARBEIDSLISTE);
    }

    @Test
    public void skalOppretteOgSletteListe() throws Exception {
        skalOppretteArbeidsliste();
        assertFalse(DB.queryForList("select * from ARBEIDSLISTE").isEmpty());

        List<ArbeidslisteRequest> arbeidslisteRequests = new ArrayList<>();
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR));
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR_2));

        Response response = arbeidsListeRessurs.deleteArbeidslisteListe(arbeidslisteRequests);

        assertEquals(200, response.getStatus());
        assertTrue(DB.queryForList("select * from ARBEIDSLISTE").isEmpty());
    }

    @Test(expected = BadRequestException.class)
    public void ugyldigFnrSkalGiBadRequest() {
        arbeidsListeRessurs.deleteArbeidsliste("UGYLDIG_FNR");
    }

    @Test
    public void responseSkalInneholdeListeMedFnr() throws Exception {
        skalOppretteArbeidsliste();
        List<ArbeidslisteRequest> arbeidslisteRequests = new ArrayList<>();
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR));
        arbeidslisteRequests.add(new ArbeidslisteRequest().setFnr(FNR_2));

        Response response = arbeidsListeRessurs.deleteArbeidslisteListe(arbeidslisteRequests);

        RestResponse<?> responseEntity = (RestResponse<?>) response.getEntity();
        assertTrue(responseEntity.getData().containsAll(asList(FNR, FNR_2)));
    }

    @Test
    public void skalOppretteArbeidsliste() {
        insertSuccessfulBrukere();
        String time = DateUtils.toIsoUTC(new Timestamp(Instant.now().plusSeconds(100000).toEpochMilli()).toLocalDateTime());

        List<ArbeidslisteRequest> arbeidslisteRequests = asList(
                new ArbeidslisteRequest().setFnr(FNR).setKommentar("Dette er en kommentar").setFrist(time),
                new ArbeidslisteRequest().setFnr(FNR_2).setKommentar("Dette er en kommentar2").setFrist(time)
        );
        Response response = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequests);

        assertEquals(201, response.getStatus());

    }

    @Test
    public void skalIkkeHatilgang() {
        insertSuccessfulBrukere();
        insertUnauthorizedBruker();

        List<ArbeidslisteRequest> arbeidslisteRequests = asList(
                new ArbeidslisteRequest().setFnr(FNR_UNAUTHORIZED).setKommentar("Dette er en kommentar").setFrist("2017-10-10T00:00:00Z"),
                new ArbeidslisteRequest().setFnr(FNR_2).setKommentar("Dette er en kommentar2").setFrist("2017-10-10T00:00:00Z")
        );
        Response response = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequests);

        assertEquals(403, response.getStatus());
    }

    @Test
    public void skalReturnereUtcStreng() {
        insertSuccessfulBrukere();
        String expectedUtcString = "2017-10-10T00:00:00Z";

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR).setKommentar("Dette er en kommentar").setFrist(expectedUtcString);
        arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequest, FNR);
        Arbeidsliste response = arbeidsListeRessurs.getArbeidsListe(FNR);

        assertEquals(expectedUtcString, response.getFrist().format(DateTimeFormatter.ISO_INSTANT));
    }

    @Test
    public void skalReturnereOppfolgendeVeilederFlagg() {
        insertSuccessfulBrukere();
        String expectedUtcString = "2017-10-10T00:00:00Z";

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR).setKommentar("Dette er en kommentar").setFrist(expectedUtcString);
        arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequest, FNR);

        Arbeidsliste response = arbeidsListeRessurs.getArbeidsListe(FNR);
        assertTrue(response.getIsOppfolgendeVeileder());
    }


    @Test
    public void skalReturnereTomArbeidslisteUthenting() {
        insertSuccessfulBrukere();

        Arbeidsliste arbeidsListe = arbeidsListeRessurs.getArbeidsListe(FNR);

        assertNull(arbeidsListe.getArbeidslisteAktiv());
    }

    @Test
    public void skalReturnereTomArbeidslisteSelvOmVeilederOgEllerEnhetMangler() {
        Arbeidsliste arbeidsListe = arbeidsListeRessurs.getArbeidsListe(FNR);

        assertNull(arbeidsListe.getArbeidslisteAktiv());
    }

    @Test(expected = BadRequestException.class)
    public void skalIkkeGodtaUgyldigFnr() {
        arbeidsListeRessurs.getArbeidsListe("123");
    }

    @Test
    public void skalKunneOppretteSammeArbeidslisteToGanger() {
        insertSuccessfulBrukere();

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR).setKommentar("Dette er en kommentar").setFrist("2017-10-10T00:00:00Z");
        Response response = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequest, FNR);
        Response response1 = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequest, FNR);

        assertEquals(201, response.getStatus());
        assertEquals(201, response1.getStatus());
    }

    @Test(expected = ForbiddenException.class)
    public void skalHaTilgangsKontrollOpprett() {
        insertUnauthorizedBruker();

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR_UNAUTHORIZED).setKommentar("Dette er en kommentar").setFrist("2017-10-10T00:00:00Z");
        arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequest, FNR_UNAUTHORIZED);
    }

    @Test(expected = ForbiddenException.class)
    public void skalHaTilgangsKontrollOppdater() {
        insertUnauthorizedBruker();

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR_UNAUTHORIZED).setKommentar("Dette er en kommentar").setFrist("2017-10-10T00:00:00Z");
        arbeidsListeRessurs.oppdaterArbeidsListe(arbeidslisteRequest, FNR_UNAUTHORIZED);
    }

    @Test(expected = ForbiddenException.class)
    public void skalHaTilgangsKontrollSlett() {
        insertUnauthorizedBruker();

        arbeidsListeRessurs.deleteArbeidsliste(FNR_UNAUTHORIZED);
    }

    @Test
    public void skalHaTilgangsKontrollHent() {
        insertUnauthorizedBruker();

        Arbeidsliste arbeidsListe = arbeidsListeRessurs.getArbeidsListe(FNR_UNAUTHORIZED);
        assertFalse(arbeidsListe.getHarVeilederTilgang());
    }

    @Test
    public void datoFeltSkalVaereValgfritt() {
        insertSuccessfulBrukere();

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR).setKommentar("Dette er en kommentar");
        Response response = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequest, FNR);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void datoSkalVaereFramITid() {
        insertSuccessfulBrukere();

        List<ArbeidslisteRequest> arbeidslisteRequests = singletonList(new ArbeidslisteRequest().setFnr(FNR).setKommentar("Dette er en kommentar").setFrist("1985-07-23T00:00:00Z"));
        Response response = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequests);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void datoSkalKunneSettesTilbakeITidVedRedigering() {
        insertSuccessfulBrukere();

        List<ArbeidslisteRequest> arbeidslisteRequests = singletonList(new ArbeidslisteRequest().setFnr(FNR_2).setKommentar("Dette er en kommentar").setFrist("2100-07-23T00:00:00Z"));
        Response response = arbeidsListeRessurs.opprettArbeidsListe(arbeidslisteRequests);
        assertEquals(201, response.getStatus());

        ArbeidslisteRequest arbeidslisteRequest = new ArbeidslisteRequest().setFnr(FNR_2).setKommentar("Dette er en kommentar").setFrist("1985-07-23T00:00:00Z");
        arbeidsListeRessurs.oppdaterArbeidsListe(arbeidslisteRequest, FNR_2);
    }

    private void insertSuccessfulBrukere() {
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

    private void insertUnauthorizedBruker() {
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
