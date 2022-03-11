package no.nav.pto.veilarbportefolje.database;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.sbl.sql.SqlUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class BrukerRepositoryTest {
    private static JdbcTemplate jdbcTemplate;
    private static BrukerRepository brukerRepository;

    private final int ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA = 51;

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();

        jdbcTemplate = new JdbcTemplate(ds);
        brukerRepository = new BrukerRepository(jdbcTemplate,  mock(UnleashService.class));
    }

    @Before
    public void setUp() {
        try {
            List<String> lines = IOUtils.readLines(Objects.requireNonNull(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql")));
            this.jdbcTemplate.execute(Joiner.on("\n").join(lines));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table bruker_data");
    }

    @Test
    public void skal_hente_bruker_fra_view() {
        final Fnr fnr = randomFnr();
        final AktorId aktorId = randomAktorId();
        final PersonId personId = randomPersonId();
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();

        SqlUtils.insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktorId.toString())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.toString())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();

        final Optional<OppfolgingsBruker> bruker = brukerRepository.hentBrukerFraView(brukerRepository.hentAktorIdFraView(fnr).get());
        assertThat(bruker).isPresent();
    }

    @Test
    public void skal_returnere_riktig_antall_brukere() {
        int antallBrukere = brukerRepository.hentAntallBrukereUnderOppfolging().orElseThrow(IllegalStateException::new);
        assertThat(antallBrukere).isEqualTo(ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA);
    }

    @Test
    public void skal_returnere_true_for_bruker_som_har_oppfolgingsflagg_satt() throws SQLException {
        ResultSet rsMock = mock(ResultSet.class);
        Mockito.when(rsMock.getString("formidlingsgruppekode")).thenReturn("foo");
        Mockito.when(rsMock.getString("kvalifiseringsgruppekode")).thenReturn("bar");
        Mockito.when(rsMock.getString("OPPFOLGING")).thenReturn("J");

        boolean result = brukerRepository.erUnderOppfolging(rsMock);
        assertThat(result).isTrue();
    }

    @Test
    public void skalKunHaEnCelleIIndekseringLogg() {
        List<Map<String, Object>> sistIndeksert = jdbcTemplate.queryForList("SELECT SIST_INDEKSERT FROM metadata");

        assertThat(sistIndeksert.size()).isEqualTo(1);
        assertThat(sistIndeksert.get(0).size()).isEqualTo(1);
    }

    @Test
    public void skalOppdatereSistIndeksertMedNyttTidsstempel() {
        Timestamp nyttTidsstempel = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(brukerRepository.updateSistIndeksertSQL(), nyttTidsstempel);

        Object sist_indeksert = jdbcTemplate.queryForList(brukerRepository.retrieveSistIndeksertSQL()).get(0).get("sist_indeksert");

        assertThat(sist_indeksert).isEqualTo(nyttTidsstempel);
    }

    @Test
    public void skalHenteVeilederForBruker() {
        AktorId aktoerId = AktorId.of("101010");
        VeilederId expectedVeilederId = VeilederId.of("X11111");

        insert(jdbcTemplate, "OPPFOLGING_DATA")
                .value("AKTOERID", aktoerId.toString())
                .value("VEILEDERIDENT", expectedVeilederId.toString())
                .execute();

        Try<VeilederId> result = brukerRepository.retrieveVeileder(aktoerId);
        assertTrue(result.isSuccess());
        assertEquals(expectedVeilederId, result.get());
    }

    @Test
    public void skalHenteEnhetForBruker() {
        Fnr fnr = Fnr.ofValidFnr("12345678900");
        String expectedEnhet = "123";

        insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", "123456")
                .value("FODSELSNR", fnr.toString())
                .value("NAV_KONTOR", expectedEnhet)
                .execute();

        Optional<String> navKontor = brukerRepository.hentNavKontorFraDbLinkTilArena(fnr);
        assertTrue(navKontor.isPresent());
        assertEquals(expectedEnhet, navKontor.get());
    }

    @Test
    public void skalHentePersonIdFraDatabase()  {
        Fnr fnr = Fnr.ofValidFnr("12345678900");

        PersonId expectedPersonId = PersonId.of("123456");
        insertOppfolgingsbrukerForPersonIdToFnrMapping(fnr, expectedPersonId);

        Optional<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertTrue(result.isPresent());
        assertEquals(expectedPersonId, result.get());
    }

    private int insertOppfolgingsbrukerForPersonIdToFnrMapping(Fnr fnr, PersonId personId) {
        return insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", personId.toString())
                .value("FODSELSNR", fnr.toString())
                .value("NAV_KONTOR", "123")
                .execute();
    }

    @Test
    public void skalIkkeFeileOmIngenPersonIdFinnes() {
        Fnr fnr = Fnr.ofValidFnr("99999999999");
        Optional<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);

        assertTrue(result.isEmpty());
    }

    @Test
    public void skalHenteFnrForPersonIdFraDatabase() throws Exception {
        PersonId personId = PersonId.of("123456");

        Fnr expectedFnr = Fnr.ofValidFnr("12345678900");

        insertOppfolgingsbrukerForPersonIdToFnrMapping(expectedFnr, personId);

        Try<Fnr> result = brukerRepository.retrieveFnrFromPersonid(personId);
        assertTrue(result.isSuccess());
        assertEquals(expectedFnr, result.get());
    }

    @Test
    public void skalIkkeFeileOmIngenFnrForPersonIdFinnes() throws Exception {
        Try<Fnr> result = brukerRepository.retrieveFnrFromPersonid(PersonId.of("123456"));

        assertNull(result.get());
    }

}
