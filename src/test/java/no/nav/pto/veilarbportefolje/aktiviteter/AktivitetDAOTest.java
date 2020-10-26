package no.nav.pto.veilarbportefolje.aktiviteter;

import com.google.common.base.Joiner;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.BrukerRepositoryTest;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static org.assertj.core.api.Assertions.assertThat;

public class AktivitetDAOTest {

    private static JdbcTemplate db;
    private static AktivitetDAO aktivitetDAO;

    private void insertoppfolgingsbrukerTestData() {
        try {
            db.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-tiltak.sql"), UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void init() {
        DataSource dataSource = setupInMemoryDatabase();
        db = new JdbcTemplate(dataSource);
        aktivitetDAO = new AktivitetDAO(db);

        db.execute("truncate table aktoerid_to_personid");
        db.execute("truncate table brukerstatus_aktiviteter");
        db.execute("truncate table aktiviteter");
        db.execute("truncate table brukertiltak");
        db.execute("truncate table enhettiltak");
        db.execute("truncate table tiltakkodeverk");
        insertoppfolgingsbrukerTestData();
    }

    @Test
    public void skal_slette_aktivitetstatus_med_gitt_type() {
        final AktoerId aktoerId = AktoerId.of("1");
        final PersonId personId = PersonId.of("1");

        final AktivitetStatus behandlingAktivitet = new AktivitetStatus()
                .setAktoerid(aktoerId)
                .setAktiv(true)
                .setAktivitetType(behandling.toString())
                .setPersonid(personId);

        final AktivitetStatus egenAktivitet = new AktivitetStatus()
                .setAktoerid(aktoerId)
                .setAktiv(true)
                .setAktivitetType(egen.toString())
                .setPersonid(personId);

        aktivitetDAO.insertAktivitetstatuser(List.of(behandlingAktivitet, egenAktivitet));
        final Map<PersonId, Set<AktivitetStatus>> aktivitetStatuser = aktivitetDAO.getAktivitetstatusForBrukere(List.of(personId));

        assertThat(aktivitetStatuser.get(personId)).hasSize(2);

        aktivitetDAO.slettAlleAktivitetstatus(behandling.toString());
        final Map<PersonId, Set<AktivitetStatus>> result = aktivitetDAO.getAktivitetstatusForBrukere(List.of(personId));

        assertThat(result.get(personId)).hasSize(1);
        assertThat(result.get(personId)).contains(egenAktivitet);

    }

    @Test
    public void skal_slette_aktivitet_med_gitt_id() {
        final String id = "1";
        final AktoerId aktoerId = AktoerId.of("1");

        final int rowsUpdated = insertAktivitet(id, aktoerId, behandling);

        assertThat(rowsUpdated).isEqualTo(1);

        aktivitetDAO.deleteById(id);

        final String result = SqlUtils
                .select(db, Table.AKTIVITETER.TABLE_NAME, rs -> rs.getString(Table.AKTIVITETER.AKTOERID))
                .column("AKTOERID")
                .where(WhereClause.equals(Table.AKTIVITETER.AKTIVITETID, id))
                .execute();

        assertThat(result).isNull();
    }

    private int insertAktivitet(String id, AktoerId aktoerId, AktivitetTyper type) {
        return SqlUtils.insert(db, Table.AKTIVITETER.TABLE_NAME)
                .value(Table.AKTIVITETER.AKTIVITETID, id)
                .value(Table.AKTIVITETER.AKTOERID, aktoerId.toString())
                .value(Table.AKTIVITETER.AKTIVITETTYPE, type.toString())
                .value(Table.AKTIVITETER.AVTALT, "N")
                .value(Table.AKTIVITETER.OPPDATERTDATO, Timestamp.from(now()))
                .value(Table.AKTIVITETER.STATUS, "fullfort")
                .execute();
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
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitet_id_test_2")
                .setAktorId("aktoer_id_test_2")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet3 = new KafkaAktivitetMelding()
                .setAktivitetId(aktivitetId)
                .setAktorId("aktoer_id_test_1")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktorId("aktoer_id_test_1")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);
        aktivitetDAO.upsertAktivitet(aktivitet3);

        String actualAktoerId = aktivitetDAO.getAktoerId(aktivitetId).toString();

        assertThat(actualAktoerId).isEqualTo(expectedAktoerId);
    }

    @Test
    public void skal_sette_inn_aktivitet() {
        KafkaAktivitetMelding aktivitet = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);

        aktivitetDAO.upsertAktivitet(aktivitet);

        Map<String, Object> aktivitetFraDB = db.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0);

        String status = (String) aktivitetFraDB.get("status");
        String type = (String) aktivitetFraDB.get("aktivitettype");

        assertThat(status).isEqualToIgnoringCase(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES.name());
        assertThat(type).isEqualToIgnoringCase(KafkaAktivitetMelding.AktivitetTypeData.EGEN.name());
    }

    @Test
    public void skal_oppdatere_aktivitet() {
        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.BRUKER_ER_INTERESSERT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("aktivitetid")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.SOKEAVTALE)
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.FULLFORT);

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

        String status = (String) db.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0).get("status");

        assertThat(status).isEqualToIgnoringCase(KafkaAktivitetMelding.AktivitetStatus.FULLFORT.name());

    }

    @Test
    public void skal_hente_distinkte_aktorider() {

        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId("id1")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("id2")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        aktivitetDAO.upsertAktivitet(aktivitet1);
        aktivitetDAO.upsertAktivitet(aktivitet2);

        assertThat(aktivitetDAO.getDistinctAktoerIdsFromAktivitet()).containsExactly("aktoerid");
    }

    @Test
    public void skal_hente_liste_med_aktiviteter_for_aktorid() {

        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId("id1")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId("id2")
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId("aktoerid")
                .setAvtalt(true)
                .setEndretDato(timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT);

        aktivitetDAO.upsertAktivitet(asList(aktivitet1, aktivitet2));

        AktoerAktiviteter aktoerAktiviteter = aktivitetDAO.getAktiviteterForAktoerid(AktoerId.of("aktoerid"));

        assertThat(aktoerAktiviteter.getAktiviteter().size()).isEqualTo(2);
        assertThat(aktoerAktiviteter.getAktoerid()).isEqualTo("aktoerid");
    }

    @Test
    public void skal_inserte_batch_av_aktivitetstatuser() {
        List<AktivitetStatus> statuser = new ArrayList<>();

        statuser.add(new AktivitetStatus()
                .setPersonid(PersonId.of("pid1"))
                .setAktoerid(AktoerId.of("aid1"))
                .setAktivitetType("a1")
                .setAktiv(true)
                .setNesteStart(new Timestamp(0))
                .setNesteUtlop(new Timestamp(0)));

        statuser.add(new AktivitetStatus()
                .setPersonid(PersonId.of("pid2"))
                .setAktoerid(AktoerId.of("aid2"))
                .setAktivitetType("a2")
                .setAktiv(true)
                .setNesteStart(new Timestamp(0))
                .setNesteUtlop(new Timestamp(0)));

        aktivitetDAO.insertAktivitetstatuser(statuser);

        final List<Map<String, Object>> result = db.queryForList("SELECT * FROM BRUKERSTATUS_AKTIVITETER");
        assertThat(result).hasSize(2);
    }

    @Test
    public void skal_returnere_tomt_map_dersom_ingen_bruker_har_aktivitetstatus_i_db() {
        assertThat(aktivitetDAO.getAktivitetstatusForBrukere(List.of(PersonId.of("personid")))).isEmpty();
    }

    @Test
    public void skal_hente_og_gruppere_aktivtetstatus_for_brukere() {

        final List<PersonId> personIds = List.of(PersonId.of("1"), PersonId.of("2"));

        SqlUtils.insert(db, Table.BRUKERSTATUS_AKTIVITETER.TABLE_NAME)
                .value(Table.BRUKERSTATUS_AKTIVITETER.PERSONID, personIds.get(0).toString())
                .value(Table.BRUKERSTATUS_AKTIVITETER.AKTIVITETTYPE, behandling.toString())
                .value(Table.BRUKERSTATUS_AKTIVITETER.STATUS, "1")
                .execute();

        SqlUtils.insert(db, Table.BRUKERSTATUS_AKTIVITETER.TABLE_NAME)
                .value(Table.BRUKERSTATUS_AKTIVITETER.PERSONID, personIds.get(0).toString())
                .value(Table.BRUKERSTATUS_AKTIVITETER.AKTIVITETTYPE, egen.toString())
                .value(Table.BRUKERSTATUS_AKTIVITETER.STATUS, "1")
                .execute();

        SqlUtils.insert(db, Table.BRUKERSTATUS_AKTIVITETER.TABLE_NAME)
                .value(Table.BRUKERSTATUS_AKTIVITETER.PERSONID, personIds.get(1).toString())
                .value(Table.BRUKERSTATUS_AKTIVITETER.AKTIVITETTYPE, AktivitetTyper.sokeavtale.toString())
                .value(Table.BRUKERSTATUS_AKTIVITETER.STATUS, "1")
                .execute();

        final Map<PersonId, Set<AktivitetStatus>> aktivitetstatusForBrukere = aktivitetDAO.getAktivitetstatusForBrukere(personIds);

        assertThat(aktivitetstatusForBrukere).hasSize(2);
    }

    @Test
    public void skal_hente_brukertiltak_for_liste_av_fnr() {
        Fnr fnr1 = Fnr.of("11111111111");
        Fnr fnr2 = Fnr.of("22222222222");

        List<Brukertiltak> brukertiltak = aktivitetDAO.hentBrukertiltak(asList(fnr1, fnr2));

        assertThat(brukertiltak.get(0).getTiltak().equals("T1")).isTrue();
        assertThat(brukertiltak.get(1).getTiltak().equals("T2")).isTrue();
        assertThat(brukertiltak.get(2).getTiltak().equals("T1")).isTrue();
    }
}
