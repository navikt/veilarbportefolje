package no.nav.fo.service;

import io.vavr.collection.List;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Optional;

import static no.nav.fo.database.BrukerRepository.OPPFOLGINGSBRUKER;
import static no.nav.fo.mock.AktoerServiceMock.*;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktoerServiceTest {

    @Inject
    private AktoerService aktoerService;

    @Inject
    private AktoerV2 aktoerV2;

    @Inject
    private JdbcTemplate db;

    private String FNR_FRA_SOAP_TJENESTE = "11111111111";
    private String AKTOERID_FRA_SOAP_TJENESTE = "2222";


    @Before
    public void setUp() throws Exception {

        WSHentAktoerIdForIdentResponse aktoerIdResponse = new WSHentAktoerIdForIdentResponse().withAktoerId(AKTOERID_FRA_SOAP_TJENESTE);
        when(aktoerV2.hentAktoerIdForIdent(any())).thenReturn(aktoerIdResponse);

        WSHentIdentForAktoerIdResponse identResponse = new WSHentIdentForAktoerIdResponse().withIdent(FNR_FRA_SOAP_TJENESTE);
        when(aktoerV2.hentIdentForAktoerId(any())).thenReturn(identResponse);
    }

    @After
    public void tearDown() throws Exception {
        db.execute("TRUNCATE TABLE BRUKER_DATA");
        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        db.execute("TRUNCATE TABLE AKTOERID_TO_PERSONID");
    }

    @Test
    public void skalFinnePersonIdFraDatabase() throws Exception {
        AktoerId aktoerId = new AktoerId("111");
        PersonId personId = new PersonId("222");
        int result =
                insert(db, "AKTOERID_TO_PERSONID")
                        .value("PERSONID", personId.toString())
                        .value("AKTOERID", aktoerId.toString())
                        .execute();

        assertTrue(result > 0);

        Optional<PersonId> maybePersonId = aktoerService.hentPersonidFraAktoerid(aktoerId);
        assertTrue(maybePersonId.isPresent());
        assertEquals(personId, maybePersonId.get());
    }

    @Test
    public void skalFinnePersonIdViaSoapTjeneste() throws Exception {
        AktoerId aktoerId = new AktoerId("111");
        PersonId personId = new PersonId("222");

        int result =
                insert(db, OPPFOLGINGSBRUKER)
                        .value("PERSON_ID", personId.toString())
                        .value("FODSELSNR", FNR_FRA_SOAP_TJENESTE)
                        .execute();

        assertTrue(result > 0);

        Optional<PersonId> maybePersonId = aktoerService.hentPersonidFraAktoerid(aktoerId);
        assertTrue(maybePersonId.isPresent());
        assertEquals(personId, maybePersonId.get());
    }

    @Test
    public void skalFortsetteVedFeil() throws Exception {
        List<String> aktoerIds = List.of("1", "2", "3", "4");
        int resultLength = aktoerIds
                .map(AktoerId::new)
                .map(aktoerService::hentPersonidFraAktoerid)
                .length();

        assertTrue(resultLength == aktoerIds.length());
    }

    @Test
    public void skalHenteAktoerIdFraPersonId() throws Exception {
        PersonId personId = new PersonId(PERSON_ID);
        AktoerId aktoerId = new AktoerId(AKTOER_ID);
        int result = insert(db, "AKTOERID_TO_PERSONID")
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .execute();

        assertTrue(result > 0);

        Optional<AktoerId> maybeAktoerId = aktoerService.hentAktoeridFraPersonid(PERSON_ID);
        assertTrue(maybeAktoerId.isPresent());
        assertEquals(aktoerId, maybeAktoerId.get());
    }

    @Test
    public void skalHenteAktoerIdFraFnrViaSoap() throws Exception {
        Fnr fnr = new Fnr(FNR);
        Optional<AktoerId> maybeAktoerId = aktoerService.hentAktoeridFraFnr(fnr);
        assertTrue(maybeAktoerId.isPresent());
        assertEquals(new AktoerId(AKTOERID_FRA_SOAP_TJENESTE), maybeAktoerId.get());
    }

    @Test
    public void skalHenteFnrFraAktoerIdFraDb() throws Exception {
        AktoerId aktoerId = new AktoerId(AKTOER_ID);
        Fnr fnr = new Fnr(FNR);
        int result = insert(db, "BRUKER_DATA")
                .value("PERSONID", PERSON_ID)
                .value("FNR", fnr.toString())
                .value("AKTOERID", aktoerId.toString())
                .execute();
        assertTrue(result > 0);

        Optional<Fnr> maybeFnr = aktoerService.hentFnrFraAktoerid(aktoerId);
        assertTrue(maybeFnr.isPresent());
        assertEquals(fnr, maybeFnr.get()
        );
    }
}