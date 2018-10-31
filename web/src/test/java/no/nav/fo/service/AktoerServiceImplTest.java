package no.nav.fo.service;

import io.vavr.control.Try;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Optional;

import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktoerServiceImplTest {

    private static final String AKTOER_ID = "aktoerid1";
    private static final String FNR = "00000000000";
    private static final String PERSON_ID = "111111";

    @Inject
    private AktorService aktorService;

    @Inject
    private AktoerServiceImpl aktoerService;

    @Inject
    private JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    @Before
    public void setUp() {
        reset(aktorService);
        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        db.execute("truncate table AKTOERID_TO_PERSONID");

        insert(db, "OPPFOLGINGSBRUKER")
                .value("FODSELSNR", FNR)
                .value("PERSON_ID", PERSON_ID)
                .execute();
    }

    @Test
    public void mapAktorId_skal_mappe_alle_aktorer_som_ikke_har_mapping() {
 
        insert(db, "OPPFOLGING_DATA")
            .value("AKTOERID", AKTOER_ID)
            .value("OPPFOLGING", "J")
            .execute();

        //assert no mappings exist
        assertThat(getMappedPersonidFromDb(AKTOER_ID).isFailure(), is(true));

        when(aktorService.getFnr(AKTOER_ID)).thenReturn(Optional.of(FNR));
        when(aktorService.getAktorId(FNR)).thenReturn(Optional.of(AKTOER_ID));
        aktoerService.mapAktorId();

        Try<String> mappedPersonid = getMappedPersonidFromDb(AKTOER_ID);
        assertThat(mappedPersonid.get(), is(PERSON_ID));

    }

    @Test
    @Ignore
    public void skalSetteGamleAktorIdTilIkkeGjeldeOgSetteNyeAktoerIdTilGjeldene() {

        insert(db, "AKTOERID_TO_PERSONID")
                .value("AKTOERID", AKTOER_ID)
                .value("PERSONID", PERSON_ID)
                .value("GJELDENE", 1)
                .execute();

        AktoerId nyAktoerId = AktoerId.of("11111");

        when(aktorService.getFnr(nyAktoerId.toString())).thenReturn(Optional.of(FNR));
        when(aktorService.getAktorId(FNR)).thenReturn(Optional.of(nyAktoerId.toString()));

        aktoerService.hentPersonidFraAktoerid(nyAktoerId);

        Try<String> gamleAktorId = getGamleAktoerId(PERSON_ID);
        assertEquals(gamleAktorId.get(), AKTOER_ID);

        Try<String> resultatNyAktoerId = getGjeldeneAktoerId(PERSON_ID);
        assertEquals(resultatNyAktoerId.get(), nyAktoerId.toString());

    }

    @Test
    @Ignore
    public void skalSetteGamleAktorIdTilIkkeGjeldeneOgSetteAktoerIdFraTPSTilGjeldene() {

        AktoerId aktoerId = AktoerId.of("99999");

        AktoerId nyAktoerId = AktoerId.of("11111");

        when(aktorService.getFnr(aktoerId.toString())).thenReturn(Optional.of(FNR));
        when(aktorService.getAktorId(FNR)).thenReturn(Optional.of(nyAktoerId.toString()));

        aktoerService.hentPersonidFraAktoerid(aktoerId);

        Try<String> gamleAktorId = getGamleAktoerId(PERSON_ID);
        assertEquals(gamleAktorId.get(), aktoerId.toString());

        Try<String> resultatNyAktoerId = getGjeldeneAktoerId(PERSON_ID);
        assertEquals(resultatNyAktoerId.get(), nyAktoerId.toString());
    }

    private Try<String> getMappedPersonidFromDb(String aktoerID) {
        return Try.of(() -> db.queryForObject(
                "SELECT PERSONID FROM AKTOERID_TO_PERSONID WHERE AKTOERID = ?", 
                new Object[] {aktoerID}, 
                (rs, rowNum) -> rs.getString("PERSONID")));
    }

    private Try<String> getGjeldeneAktoerId (String personId) {
        return Try.of(() -> db.queryForObject(
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ? AND GJELDENE = 1",
                new Object[] {personId},
                (rs, rowNum) -> rs.getString("AKTOERID")));
    }

    private Try<String> getGamleAktoerId (String personId) {
        return Try.of(() -> db.queryForObject(
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ? AND GJELDENE = 0",
                new Object[] {personId},
                (rs, rowNum) -> rs.getString("AKTOERID")));
    }

 
}