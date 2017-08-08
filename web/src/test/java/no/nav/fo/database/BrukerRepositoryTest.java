package no.nav.fo.database;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.Aktivitet.AktivitetDTO;
import no.nav.fo.domene.Aktivitet.AktivitetData;
import no.nav.fo.domene.Aktivitet.AktivitetTyper;
import no.nav.fo.domene.Aktivitet.AktoerAktiviteter;
import no.nav.fo.domene.*;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
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
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        jdbcTemplate.execute("truncate table brukerstatus_aktiviteter");
        jdbcTemplate.execute("truncate table aktiviteter");
    }

    @Test
    public void skalHenteUtAlleBrukereFraDatabasen() {
        insertoppfolgingsbrukerTestData();
        List<Map<String, Object>> brukere = jdbcTemplate.queryForList(brukerRepository.retrieveBrukereSQL());

        assertThat(brukere.size()).isEqualTo(72);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtAlleBrukere() {
        insertoppfolgingsbrukerTestData();
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(brukerRepository.retrieveBrukereSQL()).get(0).keySet();
        String[] skalHaDatabaseFelter = new String[]{"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
                "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
                "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL", "VEILEDERIDENT", "YTELSE",
                "UTLOPSDATO", "UTLOPSDATOFASETT", "AAPMAXTID", "AAPMAXTIDFASETT", "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET", "IAVTALTAKTIVITET"};

        assertThat(faktiskeDatabaseFelter).containsExactly(skalHaDatabaseFelter);
    }

    @Test
    public void skalHenteKunNyesteBrukereFraDatabasen() {
        insertoppfolgingsbrukerTestData();
        jdbcTemplate.update("UPDATE METADATA SET SIST_INDEKSERT = ?", timestampFromISO8601("2017-01-16T00:00:00Z"));

        List<Map<String, Object>> nyeBrukere = jdbcTemplate.queryForList(brukerRepository.retrieveOppdaterteBrukereSQL());
        jdbcTemplate.queryForList(brukerRepository.retrieveSistIndeksertSQL());
        assertThat(nyeBrukere.size()).isEqualTo(4);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtNyeBrukere() {
        insertoppfolgingsbrukerTestData();
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(brukerRepository.retrieveOppdaterteBrukereSQL()).get(0).keySet();
        String[] skalHaDatabaseFelter = new String[]{"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
                "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
                "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL", "VEILEDERIDENT",
                "YTELSE", "UTLOPSDATO", "UTLOPSDATOFASETT", "AAPMAXTID", "AAPMAXTIDFASETT", "OPPFOLGING", "VENTERPASVARFRABRUKER", "VENTERPASVARFRANAV", "NYESTEUTLOPTEAKTIVITET", "IAVTALTAKTIVITET"};

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
        insertoppfolgingsbrukerTestData();
        List<Map<String, Object>> mapping = brukerRepository.retrievePersonid("11111111");
        String personid = (String) mapping.get(0).get("PERSONID");
        Assertions.assertThat(personid).isEqualTo("222222");
    }

    @Test
    public void skalOppdatereOmBrukerFinnes() {
        Brukerdata brukerdata1 = brukerdata("aktoerid", "personid", "veielderid", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(), ManedMapping.MND1, LocalDateTime.now(), KvartalMapping.KV1, true);
        Brukerdata brukerdata2 = brukerdata("aktoerid", "personid", "veielderid2", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(), ManedMapping.MND1, LocalDateTime.now(), KvartalMapping.KV1, true);

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

        Brukerdata brukerdata = brukerdata("aktoerid", "personid", "veielderid", Timestamp.from(Instant.now()), YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(), ManedMapping.MND1, LocalDateTime.now(), KvartalMapping.KV1, true);

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
    public void skalVareOppfolgningsbrukerPgaOppfolgingsflagg() throws Exception {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("oppfolging", true);

        assertThat(BrukerRepository.erOppfolgingsBruker(document)).isTrue();
    }


    @Test
    public void skalFiltrereBrukere() {
        insertoppfolgingsbrukerTestData();
        jdbcTemplate.update("UPDATE METADATA SET SIST_INDEKSERT = ?", timestampFromISO8601("2017-01-16T00:00:00Z"));


        List<SolrInputDocument> aktiveBrukere = new ArrayList<>();
        brukerRepository.prosesserBrukere(3, BrukerRepository::erOppfolgingsBruker, aktiveBrukere::add);
        assertThat(aktiveBrukere.size()).isEqualTo(50);

        List<SolrInputDocument> oppdaterteAktiveBrukere = brukerRepository.retrieveOppdaterteBrukere();
        assertThat(oppdaterteAktiveBrukere.size()).isEqualTo(2);
    }

    private Brukerdata brukerdata(String aktoerid, String personId, String veileder, Timestamp tildeltTidspunkt, YtelseMapping ytelse,
                                  LocalDateTime utlopsdato, ManedMapping utlopsdatoFasett, LocalDateTime aapMaxtid, KvartalMapping aapMaxtidFasett, boolean oppfolging) {
        return new Brukerdata()
                .setAktoerid(aktoerid)
                .setPersonid(personId)
                .setVeileder(veileder)
                .setTildeltTidspunkt(tildeltTidspunkt)
                .setAapMaxtidFasett(aapMaxtidFasett)
                .setAapMaxtid(aapMaxtid)
                .setUtlopsdatoFasett(utlopsdatoFasett)
                .setUtlopsdato(utlopsdato)
                .setYtelse(ytelse)
                .setOppfolging(oppfolging);
    }

    private void assertThatBrukerdataIsEqual(Brukerdata b1, Brukerdata b2) {
        assertThat(b1.getPersonid()).isEqualTo(b2.getPersonid());
        assertThat(b1.getAapMaxtid()).isEqualTo(b2.getAapMaxtid());
        assertThat(b1.getAktoerid()).isEqualTo(b2.getAktoerid());
        assertThat(b1.getAapMaxtidFasett()).isEqualTo(b2.getAapMaxtidFasett());
        assertThat(b1.getTildeltTidspunkt()).isEqualTo(b2.getTildeltTidspunkt());
        assertThat(b1.getVeileder()).isEqualTo(b2.getVeileder());
        assertThat(b1.getUtlopsdato()).isEqualTo(b2.getUtlopsdato());
        assertThat(b1.getUtlopsdatoFasett()).isEqualTo(b2.getUtlopsdatoFasett());
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
    public void skalReturnereNullPaaAlleStatuserDersomBrukerIkkeFinnes() {
        Map<String, Timestamp> statuser = brukerRepository.getAktivitetStatusMap("jegfinnesikke");

        statuser.forEach((key, value) -> {
            assertThat(value).isNull();
            assertThat(statuser).containsKey(key);
        });
    }

    @Test
    public void skalReturnereKorrektStatusPaaAktivitet() {
        List<AktivitetTyper> aktivitetTyper = AktivitetData.aktivitetTyperList;
        Map<String, Boolean> aktivitetTypeTilStatus = new HashMap<>();
        aktivitetTypeTilStatus.put(aktivitetTyper.get(0).toString(), false);
        aktivitetTypeTilStatus.put(aktivitetTyper.get(1).toString(), true);

        brukerRepository.upsertAktivitetStatuserForBruker(aktivitetTypeTilStatus, "aktoerid", "personid");
        Map<String, Timestamp> typeTilTimestamp = brukerRepository.getAktivitetStatusMap("personid");

        assertThat(typeTilTimestamp.get(aktivitetTyper.get(0).toString())).isNull();
        assertThat(typeTilTimestamp.get(aktivitetTyper.get(1).toString())).isNotNull();
    }

    @Test
    public void skalHenteAlleAktiviteterForBruker() {
        String aktivitettpe = AktivitetData.aktivitetTyperList.get(0).toString();

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

        List<AktivitetDTO> aktiviteter = brukerRepository.getAktiviteterForAktoerid("aktoerid");

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
        String aktivitettype = AktivitetData.aktivitetTyperList.get(0).toString();

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
                .setAapMaxtid(LocalDateTime.now())
                .setAapMaxtidFasett(KvartalMapping.KV1)
                .setAktoerid("aktoerid")
                .setOppfolging(true)
                .setTildeltTidspunkt(Timestamp.from(Instant.now()))
                .setUtlopsdato(LocalDateTime.now())
                .setUtlopsdatoFasett(ManedMapping.MND1)
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
    public void skalFeileOmIngenPersonIdFinnes() throws Exception {
        Fnr fnr = new Fnr("99999999999");
        Try<PersonId> result = brukerRepository.retrievePersonidFromFnr(fnr);
        assertTrue(result.isFailure());
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
}
