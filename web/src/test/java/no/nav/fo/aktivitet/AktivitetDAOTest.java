package no.nav.fo.aktivitet;

import com.google.common.base.Joiner;
import io.vavr.Tuple;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepositoryTest;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktivitetDAOTest {

    private void insertoppfolgingsbrukerTestData() {
        try {
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-aktoerid-to-personid-testdata.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-tiltak.sql"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void deleteData() {
        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table brukerstatus_aktiviteter");
        jdbcTemplate.execute("truncate table aktiviteter");
        jdbcTemplate.execute("truncate table brukertiltak");
        jdbcTemplate.execute("truncate table enhettiltak");
        jdbcTemplate.execute("truncate table tiltakkodeverk");
        insertoppfolgingsbrukerTestData();
    }

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private AktivitetDAO aktivitetDAO;

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

        aktivitetDAO.upsertAktivitet(aktivitet);

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

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

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


        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

        List<AktivitetDTO> aktiviteter = aktivitetDAO.getAktiviteterForAktoerid(AktoerId.of("aktoerid"));

        assertThat(aktiviteter).contains(aktivitetDTO1);
        assertThat(aktiviteter).contains(aktivitetDTO2);
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

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

        assertThat(aktivitetDAO.getDistinctAktoerIdsFromAktivitet()).containsExactly("aktoerid");
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

        aktivitetDAO.upsertAktivitet(asList(aktivitet1,aktivitet2, aktivitet3, aktivitet4));

        List<AktoerAktiviteter> aktoerAktiviteter = aktivitetDAO.getAktiviteterForListOfAktoerid(asList("aktoerid1", "aktoerid2"));

        assertThat(aktoerAktiviteter.size()).isEqualTo(2);
        aktoerAktiviteter.forEach( aktoerAktivitet -> assertThat(asList("aktoerid1", "aktoerid2").contains(aktoerAktivitet.getAktoerid())));
    }

    @Test
    public void skalOppdatereAktivitetstatusForBruker() {
        String aktivitetstype1 = aktivitetTyperList.get(0).toString();
        String aktivitetstype2 = aktivitetTyperList.get(1).toString();
        PersonId personId1 = PersonId.of("personid1");
        AktoerId aktoerId1 = AktoerId.of("aktoerid1");

        PersonId personId2 = PersonId.of("personid2");
        AktoerId aktoerId2 = AktoerId.of("aktoerid2");


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

        aktiviteter1.forEach(aktivitetDAO::upsertAktivitetStatus);
        aktiviteter2.forEach(aktivitetDAO::upsertAktivitetStatus);

        Map<PersonId, Set<AktivitetStatus>> aktivitetStatuser = aktivitetDAO.getAktivitetstatusForBrukere(asList(personId1, personId2));

        assertThat(aktivitetStatuser.get(personId1)).containsExactlyInAnyOrder(a1, a2);
        assertThat(aktivitetStatuser.get(personId2)).containsExactlyInAnyOrder(b1, b2);
    }

    @Test
    public void skalInserteBatchAvAktivitetstatuser() {
        List<AktivitetStatus> statuser = new ArrayList<>();
        statuser.add(AktivitetStatus.of(PersonId.of("pid1"), AktoerId.of("aid1"),"a1",true, new Timestamp(0)));
        statuser.add(AktivitetStatus.of(PersonId.of("pid2"), AktoerId.of("aid2"),"a2",true, new Timestamp(0)));

        aktivitetDAO.insertAktivitetstatuser(statuser);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM BRUKERSTATUS_AKTIVITETER").size()).isEqualTo(2);
    }

    @Test
    public void skalReturnereTomtMapDersomIngenBrukerHarAktivitetstatusIDB() {
        assertThat(aktivitetDAO.getAktivitetstatusForBrukere(asList(PersonId.of("personid")))).isEqualTo(new HashMap<>());
    }

    @Test
    public void skalUpdateAktivteterSomAlleredeFinnes() {
        String aktivitetstype1 = aktivitetTyperList.get(0).toString();
        String aktivitetstype2 = aktivitetTyperList.get(1).toString();
        PersonId personId = PersonId.of("111111");
        AktoerId aktoerId = AktoerId.of("222222");

        AktivitetStatus a1 = AktivitetStatus.of(personId, aktoerId, aktivitetstype1, true, new Timestamp(0));
        AktivitetStatus a2 = AktivitetStatus.of(personId, aktoerId, aktivitetstype1, false, new Timestamp(0));
        AktivitetStatus a3 = AktivitetStatus.of(personId, aktoerId, aktivitetstype2, false, new Timestamp(0));


        aktivitetDAO.upsertAktivitetStatus(a1);
        aktivitetDAO.insertOrUpdateAktivitetStatus(asList(a2,a3),singletonList(Tuple.of(personId,aktivitetstype1)));

        Set<AktivitetStatus> statuser = aktivitetDAO.getAktivitetstatusForBrukere(singletonList(personId)).get(personId);

        assertThat(statuser).containsExactlyInAnyOrder(a2,a3);
    }

    @Test
    public void skalHenteBrukertiltakForListeAvFnr() {
        Fnr fnr1 = Fnr.of("11111111111");
        Fnr fnr2 = Fnr.of("22222222222");

        List<Brukertiltak> brukertiltak = aktivitetDAO.hentBrukertiltak(asList(fnr1, fnr2));

        assertThat(brukertiltak.get(0).getTiltak().equals("T1")).isTrue();
        assertThat(brukertiltak.get(1).getTiltak().equals("T2")).isTrue();
        assertThat(brukertiltak.get(2).getTiltak().equals("T1")).isTrue();
    }
}
