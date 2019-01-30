package no.nav.fo.veilarbportefolje.database;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import no.nav.fo.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.BrukerDTO;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.consumer.OppfolgingFeedHandler.OPPFOLGING_SIST_OPPDATERT;
import static no.nav.fo.veilarbportefolje.domene.AAPMaxtidUkeFasettMapping.UKE_UNDER12;
import static no.nav.fo.veilarbportefolje.domene.DagpengerUkeFasettMapping.UKE_UNDER2;
import static no.nav.fo.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static no.nav.sbl.sql.SqlUtils.insert;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class BrukerRepositoryTest {

    private static final String[] SKAL_HA_FELTER_FRA_INDEKSERINGSSPORRING = new String[]{"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
            "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
            "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL", "VEILEDERIDENT",
            "YTELSE", "UTLOPSDATO", "NY_FOR_VEILEDER", "UTLOPSDATOFASETT", "DAGPUTLOPUKE", "DAGPUTLOPUKEFASETT",
            "PERMUTLOPUKE", "PERMUTLOPUKEFASETT", "AAPMAXTIDUKE", "AAPMAXTIDUKEFASETT", "AAPUNNTAKDAGERIGJEN", "AAPUNNTAKUKERIGJENFASETT",
            "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET",
            "AKTIVITET_START", "NESTE_AKTIVITET_START", "FORRIGE_AKTIVITET_START", "MANUELL", "RESERVERTIKRR",
            "ARBEIDSLISTE_AKTIV", "ARBEIDSLISTE_KOMMENTAR", "ARBEIDSLISTE_OVERSKRIFT", "ARBEIDSLISTE_FRIST", "ARBEIDSLISTE_ENDRET_AV", "ARBEIDSLISTE_ENDRET_TID"};

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private BrukerRepository brukerRepository;

    private int ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA = 51;
    private int ANTALL_OPPDATERTE_BRUKERE_I_TESTDATA = 4;

    public void insertoppfolgingsbrukerTestData() {
        try {
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql"), UTF_8)));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-aktoerid-to-personid-testdata.sql"), UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void deleteData() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table bruker_data");
        insertoppfolgingsbrukerTestData();
    }

    @Test
    public void skal_returnere_riktig_antall_brukere_under_oppfolging() {
        List<BrukerDTO> brukereUnderOppfolging = brukerRepository.hentAlleBrukereUnderOppfolging();
        assertThat(brukereUnderOppfolging.size()).isEqualTo(ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA);
    }

    @Test
    public void skal_returnere_riktig_antall_oppdaterte_brukere() {
        jdbcTemplate.update("UPDATE METADATA SET SIST_INDEKSERT_ES = ?", timestampFromISO8601("2017-01-16T00:00:00Z"));
        List<BrukerDTO> oppdaterteBrukere = brukerRepository.hentOppdaterteBrukere();
        assertThat(oppdaterteBrukere.size()).isEqualTo(ANTALL_OPPDATERTE_BRUKERE_I_TESTDATA);
    }

    @Test
    public void skal_returnere_true_for_bruker_som_har_oppfolgingsflagg_satt() throws Exception {
        ResultSet rsMock = Mockito.mock(ResultSet.class);
        Mockito.when(rsMock.getString("formidlingsgruppekode")).thenReturn("foo");
        Mockito.when(rsMock.getString("kvalifiseringsgruppekode")).thenReturn("bar");
        Mockito.when(rsMock.getString("OPPFOLGING")).thenReturn("J");

        boolean result = brukerRepository.erUnderOppfolging(rsMock);
        assertThat(result).isTrue();
    }

    @Test
    public void skal_returnere_false_for_bruker_som_ikke_har_oppfolgingsflagg_satt() throws Exception {
        ResultSet rsMock = Mockito.mock(ResultSet.class);
        Mockito.when(rsMock.getString("formidlingsgruppekode")).thenReturn("foo");
        Mockito.when(rsMock.getString("kvalifiseringsgruppekode")).thenReturn("bar");
        Mockito.when(rsMock.getString("OPPFOLGING")).thenReturn("N");

        boolean result = brukerRepository.erUnderOppfolging(rsMock);
        assertThat(result).isFalse();
    }

    @Test
    public void skal_returnere_riktig_antall_brukere_under_oppfolging_gammel_metode() {
        ArrayList<SolrInputDocument> brukereUnderOppfolging = new ArrayList<>();
        brukerRepository.prosesserBrukere(BrukerRepository::erOppfolgingsBruker, brukereUnderOppfolging::add);
        assertThat(brukereUnderOppfolging.size()).isEqualTo(ANTALL_OPPFOLGINGSBRUKERE_I_TESTDATA);
    }

    @Test
    public void skal_returnere_riktig_antall_oppdaterte_brukere_under_oppfolging_gammel_metode() {
        jdbcTemplate.update("UPDATE METADATA SET SIST_INDEKSERT = ?", timestampFromISO8601("2017-01-16T00:00:00Z"));
        List<SolrInputDocument> brukere = brukerRepository.retrieveOppdaterteBrukere();
        long result = brukere.size();
        assertThat(result).isEqualTo(ANTALL_OPPDATERTE_BRUKERE_I_TESTDATA);
    }

    @Test
    public void skalHenteUtAlleBrukereFraDatabasen() {
        List<Map<String, Object>> brukere = jdbcTemplate.queryForList(BrukerRepository.SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO);

        assertThat(brukere.size()).isEqualTo(72);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtAlleBrukere() {
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(BrukerRepository.SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO).get(0).keySet();
        assertThat(faktiskeDatabaseFelter).containsExactly(SKAL_HA_FELTER_FRA_INDEKSERINGSSPORRING);
    }

    @Test
    public void skalHenteKunNyesteBrukereFraDatabasen() {
        jdbcTemplate.update("UPDATE METADATA SET SIST_INDEKSERT = ?", timestampFromISO8601("2017-01-16T00:00:00Z"));

        List<Map<String, Object>> nyeBrukere = jdbcTemplate.queryForList(brukerRepository.retrieveOppdaterteBrukereSQL());
        jdbcTemplate.queryForList(brukerRepository.retrieveSistIndeksertSQL());
        assertThat(nyeBrukere.size()).isEqualTo(4);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtNyeBrukere() {
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(brukerRepository.retrieveOppdaterteBrukereSQL()).get(0).keySet();
        assertThat(faktiskeDatabaseFelter).containsExactly(SKAL_HA_FELTER_FRA_INDEKSERINGSSPORRING);
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
        Brukerdata brukerdata1 = brukerdata("aktoerid", "personid", "veielderid", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(), ManedFasettMapping.MND1, 0, UKE_UNDER2, 0, UKE_UNDER2, 0, UKE_UNDER12, 2,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12, true, true);
        Brukerdata brukerdata2 = brukerdata("aktoerid", "personid", "veielderid2", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(), ManedFasettMapping.MND1, 0, UKE_UNDER2, 0, UKE_UNDER2, 0, UKE_UNDER12, 2,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12, false, true);

        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata1), emptyList());
        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata1), singletonList("personid"));
        Brukerdata brukerdataAfterInsert = brukerRepository.retrieveBrukerdata(asList("personid")).get(0);
        assertThatBrukerdataIsEqual(brukerdata1, brukerdataAfterInsert);
        brukerRepository.insertOrUpdateBrukerdata(singletonList(brukerdata2), emptyList());
        Brukerdata brukerdataAfterUpdate = brukerRepository.retrieveBrukerdata(asList("personid")).get(0);
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

    @Test
    public void skalVareOppfolgningsbrukerPgaArenaStatus() throws Exception {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("kvalifiseringsgruppekode", "BATT");
        document.addField("formidlingsgruppekode", "IARBS");
        document.addField("oppfolging", false);

        assertThat(BrukerRepository.erOppfolgingsBruker(document)).isTrue();
    }

    @Test
    public void skalIkkeVareOppfolgningsbrukerPgaFeilArenaStatusOgManglendeOppfolgingsflagg() throws Exception {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("kvalifiseringsgruppekode", "XXX");
        document.addField("formidlingsgruppekode", "ISERV");
        document.addField("oppfolging", false);

        assertThat(BrukerRepository.erOppfolgingsBruker(document)).isFalse();
    }

    @Test
    public void skalFiltrereBrukere() {
        List<SolrInputDocument> aktiveBrukere = new ArrayList<>();
        brukerRepository.prosesserBrukere(3, BrukerRepository::erOppfolgingsBruker, aktiveBrukere::add);

        assertThat(aktiveBrukere.size()).isEqualTo(51);
    }

    @Test
    public void skalVareOppfolgningsbrukerPgaOppfolgingsflagg() throws Exception {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("oppfolging", true);

        assertThat(BrukerRepository.erOppfolgingsBruker(document)).isTrue();
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
    public void skalKonvertereDokumentFeltTilBoolean() throws Exception {
        SolrInputDocument inputDocumentTrue = new SolrInputDocument();
        inputDocumentTrue.addField("oppfolging", true);

        SolrInputDocument inputDocumentFalse = new SolrInputDocument();
        inputDocumentFalse.addField("oppfolging", false);

        boolean shouldBeTrue = BrukerRepository.oppfolgingsFlaggSatt(inputDocumentTrue);
        boolean shouldBeFalse = BrukerRepository.oppfolgingsFlaggSatt(inputDocumentFalse);
        assertThat(shouldBeTrue).isTrue();
        assertThat(shouldBeFalse).isFalse();
    }

    @Test
    public void aktivitetdataSkalVaereNull() {
        Brukerdata brukerdata = new Brukerdata().setPersonid("123456");
        brukerRepository.upsertBrukerdata(brukerdata);
        jdbcTemplate.update("INSERT INTO OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) VALUES (123456, '1234567890')");

        SolrInputDocument bruker = brukerRepository.retrieveBrukermedBrukerdata("123456");

        assertThat(bruker.get("nyesteutlopteaktivitet").getValue()).isNull();
        assertThat(bruker.get("aktivitet_start").getValue()).isNull();
        assertThat(bruker.get("neste_aktivitet_start").getValue()).isNull();
        assertThat(bruker.get("forrige_aktivitet_start").getValue()).isNull();
    }

    @Test
    public void skalHenteUtAktivitetInfo() {

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("123456")
                .setNyesteUtlopteAktivitet(timestampFromISO8601("2017-01-01T13:00:00+01:00"))
                .setAktivitetStart(timestampFromISO8601("2017-01-01T14:00:00+01:00"))
                .setNesteAktivitetStart(timestampFromISO8601("2017-01-01T15:00:00+01:00"))
                .setForrigeAktivitetStart(timestampFromISO8601("2017-01-01T16:00:00+01:00"));

        brukerRepository.upsertBrukerdata(brukerdata);
        jdbcTemplate.update("INSERT INTO OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) VALUES (123456, '1234567890')");

        SolrInputDocument bruker = brukerRepository.retrieveBrukermedBrukerdata("123456");
        assertThat(bruker.get("nyesteutlopteaktivitet").getValue()).isEqualTo("2017-01-01T12:00:00Z");
        assertThat(bruker.get("aktivitet_start").getValue()).isEqualTo("2017-01-01T13:00:00Z");
        assertThat(bruker.get("neste_aktivitet_start").getValue()).isEqualTo("2017-01-01T14:00:00Z");
        assertThat(bruker.get("forrige_aktivitet_start").getValue()).isEqualTo("2017-01-01T15:00:00Z");
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
    public void skalHenteVeilederForBruker() throws Exception {
        AktoerId aktoerId = AktoerId.of("101010");
        VeilederId expectedVeilederId = VeilederId.of("X11111");

        insert(jdbcTemplate, "OPPFOLGING_DATA")
                .value("AKTOERID", aktoerId.toString())
                .value("VEILEDERIDENT", expectedVeilederId.toString())
                .execute();

        Try<VeilederId> result = brukerRepository.retrieveVeileder(aktoerId);
        assertTrue(result.isSuccess());
        assertEquals(expectedVeilederId, result.get());

        deleteData();
    }

    @Test
    public void skalHenteEnhetForBruker() throws Exception {
        Fnr fnr = new Fnr("12345678900");
        String expectedEnhet = "123";

        insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", "123456")
                .value("FODSELSNR", fnr.toString())
                .value("NAV_KONTOR", expectedEnhet)
                .execute();

        Try<String> result = brukerRepository.retrieveEnhet(fnr);
        assertTrue(result.isSuccess());
        assertEquals(expectedEnhet, result.get());
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

    @Test
    public void skalOppdatereMetadata() throws Exception {
        Date date = new Date();

        brukerRepository.updateMetadata(OPPFOLGING_SIST_OPPDATERT, date);

        Date upDated = (Date) brukerRepository.db.queryForList("SELECT oppfolging_sist_oppdatert from METADATA")
                .get(0)
                .get("oppfolging_sist_oppdatert");
        assertEquals(date, upDated);
    }

    @Test
    public void skalHenteBrukereMedBrukerdata() {
        List<PersonId> personIds = Stream.of("4120339", "4120327", "1033279", "4024027", "183651")
                .map(PersonId::of).collect(toList());

        List<SolrInputDocument> dokumenter = brukerRepository.retrieveBrukeremedBrukerdata(personIds);
        assertThat(dokumenter.size()).isEqualTo(5);
    }

}
