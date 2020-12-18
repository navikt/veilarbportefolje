package no.nav.pto.veilarbportefolje.aktiviteter;

import com.google.common.base.Joiner;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.BrukerRepositoryTest;

import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static org.assertj.core.api.Assertions.assertThat;

public class AktivitetDAOTest {

    private static JdbcTemplate jdbcTemplate;
    private static AktivitetDAO aktivitetDAO;

    private void insertoppfolgingsbrukerTestData() {
        try {
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-tiltak.sql"), UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void init() {
        DataSource dataSource = setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(dataSource);
        aktivitetDAO = new AktivitetDAO(jdbcTemplate, new NamedParameterJdbcTemplate(dataSource));

        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table brukerstatus_aktiviteter");
        jdbcTemplate.execute("truncate table aktiviteter");
        jdbcTemplate.execute("truncate table brukertiltak");
        jdbcTemplate.execute("truncate table enhettiltak");
        jdbcTemplate.execute("truncate table tiltakkodeverk");
        insertoppfolgingsbrukerTestData();
    }



    @Test(expected = IllegalStateException.class)
    public void skal_kaste_exception_ved_manglende_aktoer_id() {
        aktivitetDAO.getAktoerId("finnes_ikke");
    }

    @Test
    public void skal_hente_aktoerid_for_aktivitet() {
        String expectedAktoerId = "aktoer_id_test_1";
        String aktivitetId = "aktivitet_id_test_1";

        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId(aktivitetId)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktorId("aktoer_id_test_1")
                .setAvtalt(false)
                .setFraDato(ZonedDateTime.parse("2017-03-03T10:10:10+02:00"))
                .setTilDato(ZonedDateTime.parse("2017-12-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitet_id_test_2")
                .setAktorId("aktoer_id_test_2")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAvtalt(false)
                .setFraDato(ZonedDateTime.parse("2017-03-03T10:10:10+02:00"))
                .setTilDato(ZonedDateTime.parse("2017-12-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet3 = new KafkaAktivitetMelding()
                .setAktivitetId(aktivitetId)
                .setAktorId("aktoer_id_test_1")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktorId("aktoer_id_test_1")
                .setAvtalt(false)
                .setFraDato(ZonedDateTime.parse("2017-03-03T10:10:10+02:00"))
                .setTilDato(ZonedDateTime.parse("2017-12-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);
        aktivitetDAO.upsertAktivitet(aktivitet3);

        String actualAktoerId = aktivitetDAO.getAktoerId(aktivitetId).toString();

        assertThat(actualAktoerId).isEqualTo(expectedAktoerId);
    }

    @Test
    public void skalSetteInnAktivitet() {
        KafkaAktivitetMelding aktivitet = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(ZonedDateTime.parse("2017-03-03T10:10:10+02:00"))
                .setTilDato(ZonedDateTime.parse("2017-12-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);

        aktivitetDAO.upsertAktivitet(aktivitet);

        Map<String, Object> aktivitetFraDB = jdbcTemplate.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0);

        String status = (String) aktivitetFraDB.get("status");
        String type = (String) aktivitetFraDB.get("aktivitettype");

        assertThat(status).isEqualToIgnoringCase(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES.name());
        assertThat(type).isEqualToIgnoringCase(KafkaAktivitetMelding.AktivitetTypeData.EGEN.name());
    }

    @Test
    public void skalOppdatereAktivitet() {
        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(ZonedDateTime.parse("2017-03-03T10:10:10+02:00"))
                .setTilDato(ZonedDateTime.parse("2017-12-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.BRUKER_ER_INTERESSERT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(ZonedDateTime.parse("2017-03-03T10:10:10+02:00"))
                .setTilDato(ZonedDateTime.parse("2017-12-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.FULLFORT);

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

        String status = (String) jdbcTemplate.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0).get("status");

        assertThat(status).isEqualToIgnoringCase(KafkaAktivitetMelding.AktivitetStatus.FULLFORT.name());

    }


    @Test
    public void skalHenteDistinkteAktorider() {

        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId("id1")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("id2")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
<<<<<<< HEAD
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);;
=======
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);
>>>>>>> de28eb8573d385562b7f5d27f18459afcbe53138

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

        assertThat(aktivitetDAO.getDistinctAktoerIdsFromAktivitet()).containsExactly("aktoerid");
    }

    @Test
    public void skalHenteListeMedAktiviteterForAktorid() {
        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId("id1")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("id2")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);;

        aktivitetDAO.upsertAktivitet(asList(aktivitet1, aktivitet2));

        AktoerAktiviteter aktoerAktiviteter = aktivitetDAO.getAktiviteterForAktoerid(AktoerId.of("aktoerid"));

        assertThat(aktoerAktiviteter.getAktiviteter().size()).isEqualTo(2);
        assertThat(aktoerAktiviteter.getAktoerid()).isEqualTo("aktoerid");
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

    @Test
    public void skalHaRiktigVersionLogikk(){
        KafkaAktivitetMelding aktivitet_i_database = new KafkaAktivitetMelding()
                .setVersion(2)
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.BRUKER_ER_INTERESSERT);

        KafkaAktivitetMelding aktivitet_gammel = new KafkaAktivitetMelding()
                .setVersion(1)
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.BRUKER_ER_INTERESSERT);


        KafkaAktivitetMelding aktivitet_ny = new KafkaAktivitetMelding()
                .setVersion(3)
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.BRUKER_ER_INTERESSERT);


        assertThat(aktivitetDAO.erNyVersjonAvAktivitet(aktivitet_i_database)).isTrue();

        aktivitetDAO.upsertAktivitet(aktivitet_i_database);
        assertThat(aktivitetDAO.erNyVersjonAvAktivitet(aktivitet_gammel)).isFalse();
        assertThat(aktivitetDAO.erNyVersjonAvAktivitet(aktivitet_ny)).isTrue();
    }
}
