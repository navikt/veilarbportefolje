package no.nav.fo.database;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.*;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.consumer.OppfolgingFeedHandler.OPPFOLGING_SIST_OPPDATERT;
import static no.nav.fo.database.BrukerRepository.*;
import static no.nav.fo.domene.AAPMaxtidUkeFasettMapping.UKE_UNDER12;
import static no.nav.fo.domene.DagpengerUkeFasettMapping.UKE_UNDER2;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class BrukerRepositoryTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private BrukerRepository brukerRepository;

    public void insertoppfolgingsbrukerTestData() {
        try {
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-aktoerid-to-personid-testdata.sql"))));
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
    public void skalHenteUtAlleBrukereFraDatabasen() {
        List<Map<String, Object>> brukere = jdbcTemplate.queryForList(brukerRepository.retrieveBrukereSQL());

        assertThat(brukere.size()).isEqualTo(72);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtAlleBrukere() {
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(brukerRepository.retrieveBrukereSQL()).get(0).keySet();
        String[] skalHaDatabaseFelter = new String[]{"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
            "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
            "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL", "VEILEDERIDENT", "YTELSE",
            "UTLOPSDATO", "UTLOPSDATOFASETT", "DAGPUTLOPUKE", "DAGPUTLOPUKEFASETT",
            "PERMUTLOPUKE", "PERMUTLOPUKEFASETT", "AAPMAXTIDUKE", "AAPMAXTIDUKEFASETT", "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET"};

        assertThat(faktiskeDatabaseFelter).containsExactly(skalHaDatabaseFelter);
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
        String[] skalHaDatabaseFelter = new String[]{"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
            "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
            "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL", "VEILEDERIDENT",
            "YTELSE", "UTLOPSDATO", "UTLOPSDATOFASETT", "DAGPUTLOPUKE", "DAGPUTLOPUKEFASETT",
            "PERMUTLOPUKE", "PERMUTLOPUKEFASETT", "AAPMAXTIDUKE", "AAPMAXTIDUKEFASETT", "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET"};

        assertThat(faktiskeDatabaseFelter).containsExactly(skalHaDatabaseFelter);
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
        jdbcTemplate.update(brukerRepository.updateTidsstempelSQL(), nyttTidsstempel);

        Object sist_indeksert = jdbcTemplate.queryForList(brukerRepository.retrieveSistIndeksertSQL()).get(0).get("sist_indeksert");

        assertThat(sist_indeksert).isEqualTo(nyttTidsstempel);
    }

    @Test
    public void skalOppdatereOmBrukerFinnes() {
        Brukerdata brukerdata1 = brukerdata("aktoerid", "personid", "veielderid", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
            LocalDateTime.now(), ManedFasettMapping.MND1, 0, UKE_UNDER2, 0, UKE_UNDER2, 0, UKE_UNDER12, true);
        Brukerdata brukerdata2 = brukerdata("aktoerid", "personid", "veielderid2", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
            LocalDateTime.now(), ManedFasettMapping.MND1, 0, UKE_UNDER2, 0, UKE_UNDER2, 0, UKE_UNDER12, true);

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
        boolean oppfolging
    ) {
        return new Brukerdata()
            .setAktoerid(aktoerid)
            .setPersonid(personId)
            .setVeileder(veileder)
            .setTildeltTidspunkt(tildeltTidspunkt)
            .setUtlopsdato(utlopsdato)
            .setUtlopsFasett(utlopsdatoFasett)
            .setDagputlopUke(dagpUtlopUke)
            .setDagputlopUkeFasett(dagpUtlopUkeFasett)
            .setPermutlopUke(permutlopUke)
            .setPermutlopUkeFasett(permutlopUkeFasett)
            .setAapmaxtidUke(aapmaxtidUke)
            .setAapmaxtidUkeFasett(aapmaxtidUkeFasett)
            .setYtelse(ytelse)
            .setOppfolging(oppfolging);

    }

    private void assertThatBrukerdataIsEqual(Brukerdata b1, Brukerdata b2) {
        assertThat(b1.getPersonid()).isEqualTo(b2.getPersonid());
        assertThat(b1.getAktoerid()).isEqualTo(b2.getAktoerid());
        assertThat(b1.getTildeltTidspunkt()).isEqualTo(b2.getTildeltTidspunkt());
        assertThat(b1.getVeileder()).isEqualTo(b2.getVeileder());
        assertThat(b1.getUtlopsdato()).isEqualTo(b2.getUtlopsdato());
        assertThat(b1.getUtlopsFasett()).isEqualTo(b2.getUtlopsFasett());
        assertThat(b1.getDagputlopUke()).isEqualTo(b2.getDagputlopUke());
        assertThat(b1.getDagputlopUkeFasett()).isEqualTo(b2.getDagputlopUkeFasett());
        assertThat(b1.getPermutlopUke()).isEqualTo(b2.getPermutlopUke());
        assertThat(b1.getPermutlopUkeFasett()).isEqualTo(b2.getPermutlopUkeFasett());
        assertThat(b1.getAapmaxtidUke()).isEqualTo(b2.getAapmaxtidUke());
        assertThat(b1.getAapmaxtidUkeFasett()).isEqualTo(b2.getAapmaxtidUkeFasett());
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
    }

    @Test
    public void skalHenteUtAktivitetInfo() {
        Timestamp nyesteUtlopte = timestampFromISO8601("2017-01-01T13:00:00+01:00");

        Brukerdata brukerdata = new Brukerdata()
            .setPersonid("123456")
            .setNyesteUtlopteAktivitet(nyesteUtlopte);

        brukerRepository.upsertBrukerdata(brukerdata);
        jdbcTemplate.update("INSERT INTO OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) VALUES (123456, '1234567890')");

        SolrInputDocument bruker = brukerRepository.retrieveBrukermedBrukerdata("123456");

        assertThat(bruker.get("nyesteutlopteaktivitet").getValue()).isEqualTo("2017-01-01T12:00:00Z");
    }

    @Test
    public void retrieveBrukerdataSkalInneholdeAlleFelter() {

        Brukerdata brukerdata = new Brukerdata()
            .setNyesteUtlopteAktivitet(Timestamp.from(Instant.now()))
            .setPersonid("personid")
            .setAapmaxtidUke(1)
            .setAapmaxtidUkeFasett(AAPMaxtidUkeFasettMapping.UKE_UNDER12)
            .setAktoerid("aktoerid")
            .setOppfolging(true)
            .setTildeltTidspunkt(Timestamp.from(Instant.now()))
            .setUtlopsdato(LocalDateTime.now())
            .setUtlopsFasett(ManedFasettMapping.MND1)
            .setVeileder("Veileder")
            .setVenterPaSvarFraBruker(LocalDateTime.now())
            .setVenterPaSvarFraNav(LocalDateTime.now())
            .setYtelse(YtelseMapping.AAP_MAXTID);

        brukerRepository.upsertBrukerdata(brukerdata);

        Brukerdata brukerdataFromDB = brukerRepository.retrieveBrukerdata(singletonList("personid")).get(0);

        assertThat(brukerdata).isEqualTo(brukerdataFromDB);

    }

    @Test
    public void skalHenteVeilederForBruker() throws Exception {
        AktoerId aktoerId = AktoerId.of("101010");
        VeilederId expectedVeilederId = VeilederId.of("X11111");

        insert(jdbcTemplate, BRUKERDATA)
            .value("PERSONID", "123456")
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

        insert(jdbcTemplate, OPPFOLGINGSBRUKER)
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
        int execute = insert(jdbcTemplate, OPPFOLGINGSBRUKER)
            .value("PERSON_ID", expectedPersonId.toString())
            .value("FODSELSNR", fnr.toString())
            .value("NAV_KONTOR", "123")
            .execute();

        assertTrue(execute > 0);

        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertTrue(result.isSuccess());
        assertEquals(expectedPersonId, result.get());
    }

    @Test
    public void skalIkkeFeileOmIngenPersonIdFinnes() throws Exception {
        Fnr fnr = new Fnr("99999999999");
        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertTrue(result.get() == null);
    }

    @Test
    public void skalInserteAktoerIdToPersonMapping() throws Exception {
        AktoerId aktoerId = AktoerId.of("99999");
        PersonId personId = PersonId.of("99999999");
        Try<Integer> result = brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId);
        assertTrue(result.isSuccess());
        assertTrue(result.get() == 1);
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
    public void skalHenteListeMedAktoerids() {
        AktoerId aktoerId1 = AktoerId.of("aktoerid1");
        AktoerId aktoerId2 = AktoerId.of("aktoerid2");

        PersonId personId1 = PersonId.of("personid1");
        PersonId personId2 = PersonId.of("personid2");
        PersonId personId3 = PersonId.of("personid3");

        brukerRepository.insertAktoeridToPersonidMapping(aktoerId1, personId1);
        brukerRepository.insertAktoeridToPersonidMapping(aktoerId2, personId2);

        Map<PersonId, Optional<AktoerId>> personIdToAktoerid = brukerRepository.hentAktoeridsForPersonids(asList(personId1, personId2, personId3));

        assertThat(personIdToAktoerid.get(personId1).get()).isEqualTo(aktoerId1);
        assertThat(personIdToAktoerid.get(personId2).get()).isEqualTo(aktoerId2);
        assertThat(personIdToAktoerid.get(personId3).isPresent()).isFalse();
    }

    @Test
    public void skalHenteListeMedPersonids() {
        AktoerId aktoerId1 = AktoerId.of("aktoerid1");
        AktoerId aktoerId2 = AktoerId.of("aktoerid2");
        AktoerId aktoerId3 = AktoerId.of("aktoerid3");

        PersonId personId1 = PersonId.of("personid1");
        PersonId personId2 = PersonId.of("personid2");

        brukerRepository.insertAktoeridToPersonidMapping(aktoerId1, personId1);
        brukerRepository.insertAktoeridToPersonidMapping(aktoerId2, personId2);

        Map<AktoerId, Optional<PersonId>> aktoeridsToPersonids = brukerRepository.hentPersonidsFromAktoerids(asList(aktoerId1, aktoerId2, aktoerId3));

        assertThat(aktoeridsToPersonids.get(aktoerId1).get()).isEqualTo(personId1);
        assertThat(aktoeridsToPersonids.get(aktoerId2).get()).isEqualTo(personId2);
        assertThat(aktoeridsToPersonids.get(aktoerId3).isPresent()).isFalse();
    }

    @Test
    public void skalHenteOppfolgignsstatusForLsite() {
        List<PersonId> personIds = Stream.of("4120339", "4120327", "1033279", "4024027", "183651")
                .map(PersonId::of).collect(toList());

        Map<PersonId, Oppfolgingstatus> personIdOppfolgingstatusMap = brukerRepository.retrieveOppfolgingstatus(personIds).get();
        assertThat(personIdOppfolgingstatusMap.size()).isEqualTo(5);
    }

    @Test
    public void skalHenteBrukereMedBrukerdata() {
        List<PersonId> personIds = Stream.of("4120339", "4120327", "1033279", "4024027", "183651")
                .map(PersonId::of).collect(toList());

        List<SolrInputDocument> dokumenter = brukerRepository.retrieveBrukeremedBrukerdata(personIds);
        assertThat(dokumenter.size()).isEqualTo(5);
    }

    @Test
    public void skalSletteBrukereMedPersonid() {
        PersonId personId1 = PersonId.of("4120339");
        PersonId personId2 = PersonId.of("4120327");
        Brukerdata brukerdata1 = new Brukerdata().setPersonid(personId1.toString());
        Brukerdata brukerdata2 = new Brukerdata().setPersonid(personId2.toString());
        brukerRepository.insertOrUpdateBrukerdata(asList(brukerdata1,brukerdata2), emptyList());

        List<Brukerdata> brukerdata = brukerRepository.retrieveBrukerdata(asList(personId1.toString(), personId2.toString()));
        assertThat(brukerdata.size()).isEqualTo(2);

        brukerRepository.deleteBrukerdataForPersonIds(asList(personId1,personId2));
        List<Brukerdata> brukerdataDeleted = brukerRepository.retrieveBrukerdata(asList(personId1.toString(), personId2.toString()));
        assertThat(brukerdataDeleted.size()).isEqualTo(0);
    }
}
