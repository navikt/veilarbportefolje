package no.nav.pto.veilarbportefolje.feedconsumer.aktivitet;

import com.google.common.base.Joiner;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerRepositoryTest;
import no.nav.pto.veilarbportefolje.domene.*;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktivitetDAOTest {

    private void insertoppfolgingsbrukerTestData() {
        try {
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-tiltak.sql"), UTF_8)));
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


    @Test(expected = IllegalStateException.class)
    public void skal_kaste_exception_ved_manglende_aktoer_id() {
        aktivitetDAO.getAktoerId("finnes_ikke");
    }

    @Test
    public void skal_hente_aktoerid_for_aktivitet() {
        String expectedAktoerId = "aktoer_id_test_1";
        String aktivitetId = "aktivitet_id_test_1";

        AktivitetDataFraFeed aktivitet1 = new AktivitetDataFraFeed()
                .setAktivitetId(aktivitetId)
                .setAktivitetType("test_type")
                .setAktorId("aktoer_id_test_1")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("PLANLAGT");

        AktivitetDataFraFeed aktivitet2 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitet_id_test_2")
                .setAktorId("aktoer_id_test_2")
                .setAktivitetType("test_type")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("PLANLAGT");

        AktivitetDataFraFeed aktivitet3 = new AktivitetDataFraFeed()
                .setAktivitetId(aktivitetId)
                .setAktorId("aktoer_id_test_1")
                .setAktivitetType("test_type")
                .setAktorId("aktoer_id_test_1")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("PLANLAGT");

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);
        aktivitetDAO.upsertAktivitet(aktivitet3);

        String actualAktoerId = aktivitetDAO.getAktoerId(aktivitetId).toString();

        assertThat(actualAktoerId).isEqualTo(expectedAktoerId);
    }

    @Test
    public void skalSetteInnAktivitet() {
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
    public void skalOppdatereAktivitet() {
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

        aktivitetDAO.upsertAktivitet(asList(aktivitet1, aktivitet2, aktivitet3, aktivitet4));

        List<AktoerAktiviteter> aktoerAktiviteter = aktivitetDAO.getAktiviteterForListOfAktoerid(asList("aktoerid1", "aktoerid2"));

        assertThat(aktoerAktiviteter.size()).isEqualTo(2);
        aktoerAktiviteter.forEach(aktoerAktivitet -> assertThat(asList("aktoerid1", "aktoerid2").contains(aktoerAktivitet.getAktoerid())));
    }

    @Test
    public void skalInserteBatchAvAktivitetstatuser() {
        List<AktivitetStatus> statuser = new ArrayList<>();

        statuser.add(new AktivitetStatus()
                .setPersonid(PersonId.of("pid1"))
                .setAktoerid( AktoerId.of("aid1"))
                .setAktivitetType("a1")
                .setAktiv(true)
                .setNesteStart(new Timestamp(0))
                .setNesteUtlop( new Timestamp(0)));

        statuser.add(new AktivitetStatus()
                .setPersonid(PersonId.of("pid2"))
                .setAktoerid( AktoerId.of("aid2"))
                .setAktivitetType("a2")
                .setAktiv(true)
                .setNesteStart(new Timestamp(0))
                .setNesteUtlop( new Timestamp(0)));

        aktivitetDAO.insertAktivitetstatuser(statuser);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM BRUKERSTATUS_AKTIVITETER").size()).isEqualTo(2);
    }

    @Test
    public void skalReturnereTomtMapDersomIngenBrukerHarAktivitetstatusIDB() {
        assertThat(aktivitetDAO.getAktivitetstatusForBrukere(asList(PersonId.of("personid")))).isEqualTo(new HashMap<>());
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
