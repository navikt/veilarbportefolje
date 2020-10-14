package no.nav.pto.veilarbportefolje.database;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.sbl.sql.SqlUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.domene.AAPMaxtidUkeFasettMapping.UKE_UNDER12;
import static no.nav.pto.veilarbportefolje.domene.DagpengerUkeFasettMapping.UKE_UNDER2;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class BrukerRepositoryTest {
    private static JdbcTemplate jdbcTemplate;
    private static BrukerRepository brukerRepository;

    private final int ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA = 51;
    private int ANTALL_LINJER_I_TESTDATA;

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();

        jdbcTemplate = new JdbcTemplate(ds);
        brukerRepository = new BrukerRepository(jdbcTemplate, new NamedParameterJdbcTemplate(ds));
    }

    @Before
    public void setUp() {
        try {
            List<String> lines = IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql"), UTF_8);
            ANTALL_LINJER_I_TESTDATA = lines.size();
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
        final PersonId personId = randomPersonId();
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();

        final Optional<OppfolgingsBruker> bruker = brukerRepository.hentBrukerFraView(fnr);
        assertThat(bruker).isPresent();
    }

    @Test
    public void skal_returnere_riktig_antall_brukere_under_oppfolging() {
        List<OppfolgingsBruker> brukereUnderOppfolging = brukerRepository.hentAlleBrukereUnderOppfolging(0, ANTALL_LINJER_I_TESTDATA);
        assertThat(brukereUnderOppfolging.size()).isEqualTo(ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA);
    }

    @Test
    public void skal_hente_riktig_antall_fnr() {
        List<String> fnr = brukerRepository.hentFnrFraOppfolgingBrukerTabell(0, 10);
        assertThat(fnr.size()).isEqualTo(10);
    }

    @Test
    public void skal_ikke_tryne_om_man_proever_aa_hente_for_mange_fnr() {
        brukerRepository.hentFnrFraOppfolgingBrukerTabell(0, 10000);
    }

    @Test
    public void skal_returnere_riktig_antall_brukere() {
        int antallBrukere = brukerRepository.hentAntallBrukereUnderOppfolging().orElseThrow(IllegalStateException::new);
        assertThat(antallBrukere).isEqualTo(ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA);
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
    public void skalOppdatereOmBrukerFinnes() {
        String personId = "personid";

        Brukerdata brukerdata1 = brukerdata(
                "aktoerid",
                personId,
                "veielderid",
                Timestamp.from(Instant.now()),
                YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(),
                ManedFasettMapping.MND1,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER12,
                2,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12,
                true,
                true
        );

        Brukerdata brukerdata2 = brukerdata(
                "aktoerid",
                personId,
                "veielderid2",
                Timestamp.from(Instant.now()),
                YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(),
                ManedFasettMapping.MND1,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER12,
                2,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12,
                false,
                true
        );

        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata1), emptyList());
        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata1), singletonList(personId));

        Brukerdata brukerdataAfterInsert = brukerRepository.retrieveBrukerdata(asList(personId)).get(0);

        assertThatBrukerdataIsEqual(brukerdata1, brukerdataAfterInsert);

        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata2), emptyList());
        Brukerdata brukerdataAfterUpdate = brukerRepository.retrieveBrukerdata(asList(personId)).get(0);
        assertThatBrukerdataIsEqual(brukerdata2, brukerdataAfterUpdate);
    }


    @Test
    public void skalInserteOmBrukerIkkeFinnes() {
        Brukerdata brukerdata = brukerdata(
                "aktoerid",
                "personid",
                "veielderid",
                Timestamp.from(Instant.now()),
                YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(),
                ManedFasettMapping.MND1,
                3,
                DagpengerUkeFasettMapping.UKE2_5,
                3,
                DagpengerUkeFasettMapping.UKE2_5,
                2,
                UKE_UNDER12,
                1,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12,
                false,
                true
        );

        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata), emptyList());

        Brukerdata brukerdataFromDb = brukerRepository.retrieveBrukerdata(asList("personid")).get(0);

        assertThatBrukerdataIsEqual(brukerdata, brukerdataFromDb);
    }


    private Brukerdata brukerdata(
            String aktoerid,
            String personId,
            String veileder,
            Timestamp tildeltTidspunkt,
            YtelseMapping ytelse,
            LocalDateTime utlopsdato,
            ManedFasettMapping utlopsdatoFasett,
            Integer dagpUtlopUke,
            DagpengerUkeFasettMapping dagpUtlopUkeFasett,
            Integer permutlopUke,
            DagpengerUkeFasettMapping permutlopUkeFasett,
            Integer aapmaxtidUke,
            AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett,
            Integer aapUnntakDagerIgjen,
            AAPUnntakUkerIgjenFasettMapping aapUnntakUkerIgjenFasett,
            Boolean nyForVeileder,
            boolean oppfolging
    ) {
        return new Brukerdata()
                .setAktoerid(aktoerid)
                .setPersonid(personId)
                .setUtlopsdato(utlopsdato)
                .setUtlopsFasett(utlopsdatoFasett)
                .setDagputlopUke(dagpUtlopUke)
                .setDagputlopUkeFasett(dagpUtlopUkeFasett)
                .setPermutlopUke(permutlopUke)
                .setPermutlopUkeFasett(permutlopUkeFasett)
                .setAapmaxtidUke(aapmaxtidUke)
                .setAapmaxtidUkeFasett(aapmaxtidUkeFasett)
                .setAapUnntakDagerIgjen(aapUnntakDagerIgjen)
                .setAapunntakUkerIgjenFasett(aapUnntakUkerIgjenFasett)
                .setYtelse(ytelse);
    }

    private void assertThatBrukerdataIsEqual(Brukerdata b1, Brukerdata b2) {
        assertThat(b1.getPersonid()).isEqualTo(b2.getPersonid());
        assertThat(b1.getAktoerid()).isEqualTo(b2.getAktoerid());
        assertThat(b1.getUtlopsdato()).isEqualTo(b2.getUtlopsdato());
        assertThat(b1.getUtlopsFasett()).isEqualTo(b2.getUtlopsFasett());
        assertThat(b1.getDagputlopUke()).isEqualTo(b2.getDagputlopUke());
        assertThat(b1.getDagputlopUkeFasett()).isEqualTo(b2.getDagputlopUkeFasett());
        assertThat(b1.getPermutlopUke()).isEqualTo(b2.getPermutlopUke());
        assertThat(b1.getPermutlopUkeFasett()).isEqualTo(b2.getPermutlopUkeFasett());
        assertThat(b1.getAapmaxtidUke()).isEqualTo(b2.getAapmaxtidUke());
        assertThat(b1.getAapmaxtidUkeFasett()).isEqualTo(b2.getAapmaxtidUkeFasett());
        assertThat(b1.getAapUnntakDagerIgjen()).isEqualTo(b2.getAapUnntakDagerIgjen());
        assertThat(b1.getAapunntakUkerIgjenFasett()).isEqualTo(b2.getAapunntakUkerIgjenFasett());
        assertThat(b1.getYtelse()).isEqualTo(b2.getYtelse());
    }

    @Test
    public void retrieveBrukerdataSkalInneholdeAlleFelter() {

        Brukerdata brukerdata = new Brukerdata()
                .setNyesteUtlopteAktivitet(Timestamp.from(Instant.now()))
                .setPersonid("personid")
                .setAapmaxtidUke(1)
                .setAapmaxtidUkeFasett(AAPMaxtidUkeFasettMapping.UKE_UNDER12)
                .setAktoerid("aktoerid")
                .setUtlopsdato(LocalDateTime.now())
                .setUtlopsFasett(ManedFasettMapping.MND1)
                .setYtelse(YtelseMapping.AAP_MAXTID)
                .setAktivitetStart(new Timestamp(1))
                .setNesteAktivitetStart(new Timestamp(2))
                .setForrigeAktivitetStart(new Timestamp(3));

        brukerRepository.upsertBrukerdata(brukerdata);

        Brukerdata brukerdataFromDB = brukerRepository.retrieveBrukerdata(singletonList("personid")).get(0);

        assertThat(brukerdata).isEqualTo(brukerdataFromDB);
    }

    @Test
    public void skalHenteVeilederForBruker() {
        AktoerId aktoerId = AktoerId.of("101010");
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
        Fnr fnr = new Fnr("12345678900");
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
    public void skalHentePersonIdFraDatabase() throws Exception {
        Fnr fnr = new Fnr("12345678900");

        PersonId expectedPersonId = PersonId.of("123456");
        insertOppfolgingsbrukerForPersonIdToFnrMapping(fnr, expectedPersonId);

        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertTrue(result.isSuccess());
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
    public void skalIkkeFeileOmIngenPersonIdFinnes() throws Exception {
        Fnr fnr = new Fnr("99999999999");
        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);

        assertTrue(result.get() == null);
    }

    @Test
    public void skalHenteFnrForPersonIdFraDatabase() throws Exception {
        PersonId personId = PersonId.of("123456");

        Fnr expectedFnr = new Fnr("12345678900");

        insertOppfolgingsbrukerForPersonIdToFnrMapping(expectedFnr, personId);

        Try<Fnr> result = brukerRepository.retrieveFnrFromPersonid(personId);
        assertTrue(result.isSuccess());
        assertEquals(expectedFnr, result.get());
    }

    @Test
    public void skalIkkeFeileOmIngenFnrForPersonIdFinnes() throws Exception {
        Try<Fnr> result = brukerRepository.retrieveFnrFromPersonid(PersonId.of("123456"));

        assertTrue(result.get() == null);
    }

}
