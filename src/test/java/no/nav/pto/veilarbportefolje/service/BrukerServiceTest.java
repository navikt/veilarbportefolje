package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;


@RunWith(SpringJUnit4ClassRunner.class)
public class BrukerServiceTest {

    private static final String AKTOER_ID = "aktoerid1";
    private static final String FNR = "10108000398";
    private static final String PERSON_ID = "111111";

    private BrukerService brukerService;

    private JdbcTemplate db;

    private BrukerRepository brukerRepository;

    private AktorregisterClient aktorregisterClient;


    private String FNR_FRA_SOAP_TJENESTE = "11111111111";
    private String AKTOERID_FRA_SOAP_TJENESTE = "2222";

    @Before
    public void setUp() {

        db = new JdbcTemplate(setupInMemoryDatabase());
        brukerRepository = new BrukerRepository(db, null);
        aktorregisterClient = mock(AktorregisterClient.class);
        brukerService = new BrukerService(brukerRepository, aktorregisterClient);

        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        db.execute("truncate table AKTOERID_TO_PERSONID");

        insert(db, "OPPFOLGINGSBRUKER")
                .value("FODSELSNR", FNR)
                .value("PERSON_ID", PERSON_ID)
                .execute();
    }

    public void skalFinnePersonIdFraDatabase() {
        when(aktorregisterClient.hentFnr(anyString())).thenReturn(FNR_FRA_SOAP_TJENESTE);
        when(aktorregisterClient.hentAktorId(anyString())).thenReturn(AKTOERID_FRA_SOAP_TJENESTE);

        AktoerId aktoerId = AktoerId.of("111");
        PersonId personId = PersonId.of("222");
        int updated =
                insert(db, "AKTOERID_TO_PERSONID")
                        .value("PERSONID", personId.toString())
                        .value("AKTOERID", aktoerId.toString())
                        .execute();

        assertTrue(updated > 0);

        Try<PersonId> result = brukerService.hentPersonidFraAktoerid(aktoerId);
        verify(aktorregisterClient, never()).hentFnr(anyString());
        assertTrue(result.isSuccess());
        Assertions.assertEquals(personId, result.get());
    }

    @Test
    public void skalSetteGamleAktorIdTilIkkeGjeldeOgSetteNyeAktoerIdTilGjeldene() {

        insert(db, "AKTOERID_TO_PERSONID")
                .value("AKTOERID", AKTOER_ID)
                .value("PERSONID", PERSON_ID)
                .value("GJELDENE", 1)
                .execute();

        AktoerId nyAktoerId = AktoerId.of("11111");

        when(aktorregisterClient.hentFnr(nyAktoerId.toString())).thenReturn(FNR);
        when(aktorregisterClient.hentAktorId(FNR)).thenReturn(nyAktoerId.toString());

        brukerService.hentPersonidFraAktoerid(nyAktoerId);

        Try<String> gamleAktorId = getGamleAktoerId(PERSON_ID);
        assertEquals(gamleAktorId.get(), AKTOER_ID);

        Try<String> resultatNyAktoerId = getGjeldeneAktoerId(PERSON_ID);
        assertEquals(resultatNyAktoerId.get(), nyAktoerId.toString());

    }

    @Test
    public void skalSetteGamleAktorIdTilIkkeGjeldene() {

        AktoerId aktoerId = AktoerId.of("99999");

        AktoerId nyAktoerId = AktoerId.of("11111");

        when(aktorregisterClient.hentFnr(aktoerId.toString())).thenReturn(FNR);
        when(aktorregisterClient.hentAktorId(FNR)).thenReturn(nyAktoerId.toString());

        brukerService.hentPersonidFraAktoerid(aktoerId);

        Try<String> gamleAktorId = getGamleAktoerId(PERSON_ID);
        assertEquals(gamleAktorId.get(), aktoerId.toString());
    }

    private Try<String> getGjeldeneAktoerId(String personId) {
        return Try.of(() -> db.queryForObject(
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ? AND GJELDENE = 1",
                new Object[]{personId},
                (rs, rowNum) -> rs.getString("AKTOERID")));
    }

    private Try<String> getGamleAktoerId(String personId) {
        return Try.of(() -> db.queryForObject(
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ? AND GJELDENE = 0",
                new Object[]{personId},
                (rs, rowNum) -> rs.getString("AKTOERID")));
    }


}
