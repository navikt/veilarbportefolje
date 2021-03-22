package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

    private ElasticServiceV2 elasticServiceV2;

    private AktorClient aktorClient;

    private String FNR_FRA_SOAP_TJENESTE = "11111111111";
    private String AKTOERID_FRA_SOAP_TJENESTE = "2222";

    @Before
    public void setUp() {

        db = new JdbcTemplate(setupInMemoryDatabase());
        brukerRepository = new BrukerRepository(db, null,  mock(UnleashService.class));
        aktorClient = mock(AktorClient.class);
        UnleashService unleashService = mock(UnleashService.class);
        when(unleashService.isEnabled(FeatureToggle.AUTO_SLETT)).thenReturn(true);

        elasticServiceV2 = mock(ElasticServiceV2.class);
        brukerService = new BrukerService(brukerRepository, aktorClient, elasticServiceV2, unleashService);

        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        db.execute("truncate table AKTOERID_TO_PERSONID");

        insert(db, "OPPFOLGINGSBRUKER")
                .value("FODSELSNR", FNR)
                .value("PERSON_ID", PERSON_ID)
                .execute();
    }

    @Test
    public void skalFinnePersonIdFraDatabase() {
        when(aktorClient.hentFnr(any(AktorId.class))).thenReturn(Fnr.ofValidFnr(FNR_FRA_SOAP_TJENESTE));
        when(aktorClient.hentAktorId(any(Fnr.class))).thenReturn(AktorId.of(AKTOERID_FRA_SOAP_TJENESTE));

        AktorId aktoerId = AktorId.of("111");
        PersonId personId = PersonId.of("222");
        int updated =
                insert(db, "AKTOERID_TO_PERSONID")
                        .value("PERSONID", personId.toString())
                        .value("AKTOERID", aktoerId.toString())
                        .execute();

        assertTrue(updated > 0);

        Try<PersonId> result = brukerService.hentPersonidFraAktoerid(aktoerId);
        verify(aktorClient, never()).hentFnr(any(AktorId.class));
        verify(elasticServiceV2, never()).slettDokumenter(any());
        assertTrue(result.isSuccess());
        assertEquals(personId, result.get());
    }

    @Test
    public void skalSetteGamleAktorIdTilIkkeGjeldeOgSetteNyeAktorIdTilGjeldene() {

        insert(db, "AKTOERID_TO_PERSONID")
                .value("AKTOERID", AKTOER_ID)
                .value("PERSONID", PERSON_ID)
                .value("GJELDENE", 1)
                .execute();

        AktorId nyAktorId = AktorId.of("11111");

        when(aktorClient.hentFnr(nyAktorId)).thenReturn(Fnr.ofValidFnr(FNR));
        when(aktorClient.hentAktorId(Fnr.ofValidFnr(FNR))).thenReturn(nyAktorId);

        brukerService.hentPersonidFraAktoerid(nyAktorId);

        Try<String> gamleAktorId = getGamleAktorId(PERSON_ID);
        assertEquals(gamleAktorId.get(), AKTOER_ID);

        Try<String> resultatNyAktorId = getGjeldeneAktorId(PERSON_ID);
        assertEquals(resultatNyAktorId.get(), nyAktorId.toString());

        verify(elasticServiceV2).slettDokumenter(List.of(AktorId.of(AKTOER_ID)));
    }

    @Test
    public void skalSetteGamleAktorIdTilIkkeGjeldene() {

        AktorId aktoerId = AktorId.of("99999");

        AktorId nyAktorId = AktorId.of("11111");

        when(aktorClient.hentFnr(aktoerId)).thenReturn(Fnr.ofValidFnr(FNR));
        when(aktorClient.hentAktorId(Fnr.ofValidFnr(FNR))).thenReturn(nyAktorId);

        brukerService.hentPersonidFraAktoerid(aktoerId);

        Try<String> gamleAktorId = getGamleAktorId(PERSON_ID);
        assertEquals(gamleAktorId.get(), aktoerId.toString());

        verify(elasticServiceV2).slettDokumenter(List.of(aktoerId));
    }

    private Try<String> getGjeldeneAktorId(String personId) {
        return Try.of(() -> db.queryForObject(
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ? AND GJELDENE = 1",
                new Object[]{personId},
                (rs, rowNum) -> rs.getString("AKTOERID")));
    }

    private Try<String> getGamleAktorId(String personId) {
        return Try.of(() -> db.queryForObject(
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ? AND GJELDENE = 0",
                new Object[]{personId},
                (rs, rowNum) -> rs.getString("AKTOERID")));
    }


}
