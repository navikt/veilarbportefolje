package no.nav.pto.veilarbportefolje.database;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
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
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.assertj.core.api.Assertions.assertThat;

public class BrukerRepositoryTest {
    private static JdbcTemplate jdbcTemplate;
    private static BrukerRepository brukerRepository;

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();

        jdbcTemplate = new JdbcTemplate(ds);
        brukerRepository = new BrukerRepository(jdbcTemplate);
    }

    @Before
    public void setUp() {
        try {
            List<String> lines = IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql"), UTF_8);
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
        final Fnr fnr = Fnr.of("00000000000");
        final String personId = "0";

        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId)
                .execute();

        final Optional<OppfolgingsBruker> bruker = brukerRepository.hentBrukerFraView(fnr);
        assertThat(bruker).isPresent();
    }

    @Test
    public void skal_returnere_riktig_antall_oppdaterte_brukere() {
        jdbcTemplate.update("UPDATE METADATA SET SIST_INDEKSERT_ES = ?", timestampFromISO8601("2017-01-16T00:00:00Z"));
        List<OppfolgingsBruker> oppdaterteBrukere = brukerRepository.hentOppdaterteBrukere();
        int ANTALL_OPPDATERTE_BRUKERE_I_TESTDATA = 4;
        assertThat(oppdaterteBrukere.size()).isEqualTo(ANTALL_OPPDATERTE_BRUKERE_I_TESTDATA);
    }

    @Test
    public void skal_returnere_true_for_bruker_som_har_oppfolgingsflagg_satt() throws SQLException {
        ResultSet rsMock = Mockito.mock(ResultSet.class);
        Mockito.when(rsMock.getString("formidlingsgruppekode")).thenReturn("foo");
        Mockito.when(rsMock.getString("kvalifiseringsgruppekode")).thenReturn("bar");
        Mockito.when(rsMock.getString("OPPFOLGING")).thenReturn("J");

        boolean result = brukerRepository.erUnderOppfolging(rsMock);
        assertThat(result).isTrue();
    }

    @Test
    public void skal_kun_ha_en_celle_i_indeksering_logg() {
        List<Map<String, Object>> sistIndeksert = jdbcTemplate.queryForList("SELECT SIST_INDEKSERT FROM metadata");

        assertThat(sistIndeksert.size()).isEqualTo(1);
        assertThat(sistIndeksert.get(0).size()).isEqualTo(1);
    }

    @Test
    public void skal_oppdatere_sist_indeksert_med_nytt_tidsstempel() {
        Timestamp nyttTidsstempel = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(brukerRepository.updateSistIndeksertSQL(), nyttTidsstempel);

        Object sist_indeksert = jdbcTemplate.queryForList(brukerRepository.retrieveSistIndeksertSQL()).get(0).get("sist_indeksert");

        assertThat(sist_indeksert).isEqualTo(nyttTidsstempel);
    }

    @Test
    public void skal_hente_veileder_for_bruker() {
        AktoerId aktoerId = AktoerId.of("101010");
        VeilederId expectedVeilederId = VeilederId.of("X11111");

        insert(jdbcTemplate, "OPPFOLGING_DATA")
                .value("AKTOERID", aktoerId.toString())
                .value("VEILEDERIDENT", expectedVeilederId.toString())
                .execute();

        Try<VeilederId> result = brukerRepository.retrieveVeileder(aktoerId);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(expectedVeilederId);
    }

    @Test
    public void skal_hente_enhet_for_bruker() {
        Fnr fnr = new Fnr("12345678900");
        String expectedEnhet = "123";

        insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", "123456")
                .value("FODSELSNR", fnr.toString())
                .value("NAV_KONTOR", expectedEnhet)
                .execute();

        Optional<String> navKontor = brukerRepository.hentNavKontorFraDbLinkTilArena(fnr);
        assertThat(navKontor.get()).isEqualTo(expectedEnhet);
    }

    @Test
    public void skal_hente_person_id_fra_database() {
        Fnr fnr = new Fnr("12345678900");

        PersonId expectedPersonId = PersonId.of("123456");
        insertOppfolgingsbrukerForPersonIdToFnrMapping(fnr, expectedPersonId);

        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertThat(result.get()).isEqualTo(expectedPersonId);
    }

    @Test
    public void skal_ikke_feile_om_ingen_person_id_finnes() {
        Fnr fnr = new Fnr("99999999999");
        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertThat(result.get()).isNull();
    }

    @Test
    public void skalHenteFnrForPersonIdFraDatabase() {
        PersonId personId = PersonId.of("123456");
        Fnr expectedFnr = new Fnr("12345678900");

        insertOppfolgingsbrukerForPersonIdToFnrMapping(expectedFnr, personId);

        Try<Fnr> result = brukerRepository.retrieveFnrFromPersonid(personId);
        assertThat(result.get()).isEqualTo(expectedFnr);
    }

    @Test
    public void skal_ikke_feile_om_ingen_fnr_for_person_id_finnes() {
        Try<Fnr> result = brukerRepository.retrieveFnrFromPersonid(PersonId.of("123456"));
        assertThat(result.get()).isNull();
    }

    private int insertOppfolgingsbrukerForPersonIdToFnrMapping(Fnr fnr, PersonId personId) {
        return insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", personId.toString())
                .value("FODSELSNR", fnr.toString())
                .value("NAV_KONTOR", "123")
                .execute();
    }
}
