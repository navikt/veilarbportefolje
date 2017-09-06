package no.nav.fo.database;

import com.google.common.base.Joiner;
import io.vavr.Tuple;
import io.vavr.control.Try;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.fo.consumer.SituasjonFeedHandler.SITUASJON_SIST_OPPDATERT;
import static no.nav.fo.database.BrukerRepository.*;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static no.nav.fo.domene.AAPMaxtidUkeFasettMapping.UKE_UNDER12;
import static no.nav.fo.domene.DagpengerUkeFasettMapping.UKE_UNDER2;
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
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-tiltak.sql"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void deleteData() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table bruker_data");
        jdbcTemplate.execute("truncate table brukerstatus_aktiviteter");
        jdbcTemplate.execute("truncate table aktiviteter");
        jdbcTemplate.execute("truncate table brukertiltak");
        jdbcTemplate.execute("truncate table tiltakkodeverk");
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
                "PERMUTLOPUKE", "PERMUTLOPUKEFASETT", "AAPMAXTIDUKE", "AAPMAXTIDUKEFASETT", "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET", "IAVTALTAKTIVITET"};

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
                "PERMUTLOPUKE", "PERMUTLOPUKEFASETT", "AAPMAXTIDUKE", "AAPMAXTIDUKEFASETT", "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET", "IAVTALTAKTIVITET"};

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
    public void skalReturnerePersonidFraDB() {
        List<Map<String, Object>> mapping = brukerRepository.retrievePersonid("11111111");
        String personid = (String) mapping.get(0).get("PERSONID");
        Assertions.assertThat(personid).isEqualTo("222222");
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
    public void skalSetteInnAktivitet() throws Exception {
        AktivitetDataFraFeed aktivitet = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid")
                .setAktivitetType("aktivitettype")
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("STATUS");

        brukerRepository.upsertAktivitet(aktivitet);

        String status = (String) jdbcTemplate.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0).get("status");

        assertThat(status).isEqualToIgnoringCase("STATUS");
    }

    @Test
    public void skalOppdatereAktivitet() throws Exception {
        AktivitetDataFraFeed aktivitet1 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid")
                .setAktivitetType("aktivitettype")
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("IKKE STARTET");

        AktivitetDataFraFeed aktivitet2 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid")
                .setAktivitetType("aktivitettype")
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("FERDIG");

        brukerRepository.upsertAktivitet(aktivitet1);
        brukerRepository.upsertAktivitet(aktivitet2);

        String status = (String) jdbcTemplate.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0).get("status");

        assertThat(status).isEqualToIgnoringCase("FERDIG");

    }


    @Test
    public void skalHenteAlleAktiviteterForBruker() {
        String aktivitettpe = aktivitetTyperList.get(0).toString();

        AktivitetDataFraFeed aktivitet1 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid1")
                .setAktivitetType(aktivitettpe)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikke startet");

        AktivitetDataFraFeed aktivitet2 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid2")
                .setAktivitetType(aktivitettpe)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ferdig");

        AktivitetDTO aktivitetDTO1 = new AktivitetDTO()
                .setAktivitetType(aktivitettpe)
                .setStatus("ikke startet")
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"));

        AktivitetDTO aktivitetDTO2 = new AktivitetDTO()
                .setAktivitetType(aktivitettpe)
                .setStatus("ferdig");


        brukerRepository.upsertAktivitet(aktivitet1);
        brukerRepository.upsertAktivitet(aktivitet2);

        List<AktivitetDTO> aktiviteter = brukerRepository.getAktiviteterForAktoerid(AktoerId.of("aktoerid"));

        assertThat(aktiviteter).contains(aktivitetDTO1);
        assertThat(aktiviteter).contains(aktivitetDTO2);
    }

    @Test
    public void aktivitetdataSkalVaereNull() {
        Brukerdata brukerdata = new Brukerdata().setPersonid("123456");
        brukerRepository.upsertBrukerdata(brukerdata);
        jdbcTemplate.update("INSERT INTO OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) VALUES (123456, '1234567890')");


        SolrInputDocument bruker = brukerRepository.retrieveBrukermedBrukerdata("123456");

        assertThat(bruker.get("nyesteutlopteaktivitet").getValue()).isNull();
        assertThat(bruker.get("iavtaltaktivitet").getValue()).isNull();
    }

    @Test
    public void skalHenteUtAktivitetInfo() {
        Timestamp nyesteUtlopte = timestampFromISO8601("2017-01-01T13:00:00+01:00");

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("123456")
                .setIAvtaltAktivitet(true)
                .setNyesteUtlopteAktivitet(nyesteUtlopte);

        brukerRepository.upsertBrukerdata(brukerdata);
        jdbcTemplate.update("INSERT INTO OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) VALUES (123456, '1234567890')");

        SolrInputDocument bruker = brukerRepository.retrieveBrukermedBrukerdata("123456");

        assertThat(bruker.get("iavtaltaktivitet").getValue()).isEqualTo(true);
        assertThat(bruker.get("nyesteutlopteaktivitet").getValue()).isEqualTo("2017-01-01T12:00:00Z");
    }

    @Test
    public void skalHenteDistinkteAktorider() {

        AktivitetDataFraFeed aktivitet1 = new AktivitetDataFraFeed()
                .setAktivitetId("id1")
                .setAktivitetType("dontcare")
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikke startet");

        AktivitetDataFraFeed aktivitet2 = new AktivitetDataFraFeed()
                .setAktivitetId("id2")
                .setAktivitetType("dontcare")
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikke startet");

        brukerRepository.upsertAktivitet(aktivitet1);
        brukerRepository.upsertAktivitet(aktivitet2);

        assertThat(brukerRepository.getDistinctAktoerIdsFromAktivitet()).containsExactly("aktoerid");
    }

    @Test
    public void skalHenteListeMedAktiviteterForAktorids() {
        String aktivitettype = aktivitetTyperList.get(0).toString();

        AktivitetDataFraFeed aktivitet1 = new AktivitetDataFraFeed().setAktivitetId("id1").setAktivitetType(aktivitettype)
                .setAktorId("aktoerid1").setAvtalt(true).setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikkeFullfortStatus1");

        AktivitetDataFraFeed aktivitet2 = new AktivitetDataFraFeed().setAktivitetId("id2").setAktivitetType(aktivitettype)
                .setAktorId("aktoerid1").setAvtalt(true).setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikkeFullfortStatus1");

        AktivitetDataFraFeed aktivitet3 = new AktivitetDataFraFeed().setAktivitetId("id3").setAktivitetType(aktivitettype)
                .setAktorId("aktoerid2").setAvtalt(true).setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikkeFullfortStatus2");

        AktivitetDataFraFeed aktivitet4 = new AktivitetDataFraFeed().setAktivitetId("id4").setAktivitetType(aktivitettype)
                .setAktorId("aktoerid2").setAvtalt(true).setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("ikkeFullfortStatus2");

        brukerRepository.upsertAktivitet(asList(aktivitet1,aktivitet2, aktivitet3, aktivitet4));

        List<AktoerAktiviteter> aktoerAktiviteter = brukerRepository.getAktiviteterForListOfAktoerid(asList("aktoerid1", "aktoerid2"));

        assertThat(aktoerAktiviteter.size()).isEqualTo(2);
        aktoerAktiviteter.forEach( aktoerAktivitet -> assertThat(asList("aktoerid1", "aktoerid2").contains(aktoerAktivitet.getAktoerid())));
    }

    @Test
    public void retrieveBrukerdataSkalInneholdeAlleFelter() {

        Brukerdata brukerdata = new Brukerdata()
                .setNyesteUtlopteAktivitet(Timestamp.from(Instant.now()))
                .setIAvtaltAktivitet(true)
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
        AktoerId aktoerId = new AktoerId("101010");
        VeilederId expectedVeilederId = new VeilederId("X11111");

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

        PersonId expectedPersonId = new PersonId("123456");
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
        AktoerId aktoerId = new AktoerId("99999");
        PersonId personId = new PersonId("99999999");
        Try<Integer> result = brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId);
        assertTrue(result.isSuccess());
        assertTrue(result.get() == 1);
    }

    @Test
    public void skalHenteOppfolgingstatus() throws Exception {
        PersonId personId = new PersonId("123456");

        insert(jdbcTemplate, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", personId.toString())
                .value("FODSELSNR", "123123")
                .value(KVALIFISERINGSGRUPPEKODE, "TESTKODE")
                .value(FORMIDLINGSGRUPPEKODE, "TESTKODE")
                .execute();
        insert(jdbcTemplate, BRUKERDATA)
                .value("PERSONID", personId.toString())
                .value("OPPFOLGING", "J")
                .value("VEILEDERIDENT", "TESTIDENT")
                .execute();

        Oppfolgingstatus status = brukerRepository.retrieveOppfolgingstatus(personId).get();
        assertEquals(status.getFormidlingsgruppekode(), "TESTKODE");
        assertEquals(status.getServicegruppekode(), "TESTKODE");
        assertEquals(status.getVeileder(), "TESTIDENT");
        assertTrue(status.isOppfolgingsbruker());
    }

    @Test
    public void skalOppdatereMetadata() throws Exception {
        Date date = new Date();

        brukerRepository.updateMetadata(SITUASJON_SIST_OPPDATERT, date);

        Date upDated = (Date) brukerRepository.db.queryForList("SELECT situasjon_sist_oppdatert from METADATA").get(0).get("situasjon_sist_oppdatert");
        assertEquals(date, upDated);
    }

    @Test
    public void skalSletteBrukerdata() throws Exception {
        Brukerdata brukerdata = new Brukerdata()
                .setPersonid("123456")
                .setAktoerid("AKTOERID")
                .setVeileder("VEIELDER");

        brukerRepository.upsertBrukerdata(brukerdata);

        assertFalse(brukerRepository.retrieveBrukerdata(singletonList("123456")).isEmpty());

        brukerRepository.deleteBrukerdata(new PersonId("123456"));

        assertTrue(brukerRepository.retrieveBrukerdata(singletonList("123456")).isEmpty());

    }

    @Test
    public void skalHenteBrukersTiltak() throws Exception {
        assertThat(brukerRepository.getBrukertiltak("2343601")).containsExactly("A", "B");
        assertThat(brukerRepository.getBrukertiltak("2343602")).containsExactly("B");
    }

    @Test
    public void skalOppdatereAktivitetstatusForBruker() {
        String aktivitetstype1 = aktivitetTyperList.get(0).toString();
        String aktivitetstype2 = aktivitetTyperList.get(1).toString();
        PersonId personId1 = new PersonId("personid1");
        AktoerId aktoerId1 = new AktoerId("aktoerid1");

        PersonId personId2 = new PersonId("personid2");
        AktoerId aktoerId2 = new AktoerId("aktoerid2");


        AktivitetStatus a1 = AktivitetStatus.of(personId1,aktoerId1,aktivitetstype1,true, new Timestamp(0));
        AktivitetStatus a2 = AktivitetStatus.of(personId1,aktoerId1,aktivitetstype2,false, new Timestamp(0));
        AktivitetStatus b1 = AktivitetStatus.of(personId2,aktoerId2,aktivitetstype1,true, new Timestamp(0));
        AktivitetStatus b2 = AktivitetStatus.of(personId2,aktoerId2,aktivitetstype2,false, new Timestamp(0));

        Set<AktivitetStatus> aktiviteter1 = new HashSet<>();
        Set<AktivitetStatus> aktiviteter2 = new HashSet<>();
        aktiviteter1.add(a1);
        aktiviteter1.add(a2);
        aktiviteter2.add(b1);
        aktiviteter2.add(b2);

        aktiviteter1.forEach(brukerRepository::upsertAktivitetStatus);
        aktiviteter2.forEach(brukerRepository::upsertAktivitetStatus);

        Map<PersonId, Set<AktivitetStatus>> aktivitetStatuser = brukerRepository.getAktivitetstatusForBrukere(asList(personId1, personId2));

        assertThat(aktivitetStatuser.get(personId1)).containsExactlyInAnyOrder(a1, a2);
        assertThat(aktivitetStatuser.get(personId2)).containsExactlyInAnyOrder(b1, b2);
    }

    @Test
    public void skalInserteOgSletteAktivitetstatus() {
        String aktivitetstype = "aktivitetstype";
        AktivitetStatus aktivitetStatus =  AktivitetStatus.of(new PersonId("personid"), new AktoerId("aktivitetid"),aktivitetstype,true,null);
        brukerRepository.insertAktivitetStatus(aktivitetStatus);
        assertThat(brukerRepository.db.queryForList("select * from brukerstatus_aktiviteter")).isNotEmpty();
        brukerRepository.slettAlleAktivitetstatus(aktivitetstype);
        assertThat(brukerRepository.db.queryForList("select * from brukerstatus_aktiviteter")).isEmpty();
    }

    @Test
    public void skalInserteBatchAvAktivitetstatuser() {
        List<AktivitetStatus> statuser = new ArrayList<>();
        statuser.add(AktivitetStatus.of(PersonId.of("pid1"), AktoerId.of("aid1"),"a1",true, new Timestamp(0)));
        statuser.add(AktivitetStatus.of(PersonId.of("pid2"), AktoerId.of("aid2"),"a2",true, new Timestamp(0)));

        brukerRepository.insertAktivitetstatuser(statuser);
        assertThat(brukerRepository.db.queryForList("SELECT * FROM BRUKERSTATUS_AKTIVITETER").size()).isEqualTo(2);
    }

    @Test
    public void skalReturnereTomtMapDersomIngenBrukerHarAktivitetstatusIDB() {
        assertThat(brukerRepository.getAktivitetstatusForBrukere(asList(new PersonId("personid")))).isEqualTo(new HashMap<>());
    }

    @Test
    public void skalUpdateAktivteterSomAlleredeFinnes() {
        String aktivitetstype1 = aktivitetTyperList.get(0).toString();
        String aktivitetstype2 = aktivitetTyperList.get(1).toString();
        PersonId personId = new PersonId("111111");
        AktoerId aktoerId = new AktoerId("222222");

        AktivitetStatus a1 = AktivitetStatus.of(personId, aktoerId, aktivitetstype1, true, new Timestamp(0));
        AktivitetStatus a2 = AktivitetStatus.of(personId, aktoerId, aktivitetstype1, false, new Timestamp(0));
        AktivitetStatus a3 = AktivitetStatus.of(personId, aktoerId, aktivitetstype2, false, new Timestamp(0));


        brukerRepository.upsertAktivitetStatus(a1);
        brukerRepository.insertOrUpdateAktivitetStatus(asList(a2,a3),singletonList(Tuple.of(personId,aktivitetstype1)));

        Set<AktivitetStatus> statuser = brukerRepository.getAktivitetstatusForBrukere(singletonList(personId)).get(personId);

        assertThat(statuser).containsExactlyInAnyOrder(a2,a3);
    }
}
