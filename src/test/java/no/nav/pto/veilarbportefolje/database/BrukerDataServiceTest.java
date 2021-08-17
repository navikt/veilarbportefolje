package no.nav.pto.veilarbportefolje.database;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class BrukerDataServiceTest {
    private final JdbcTemplate jdbcTemplate;
    private final BrukerDataService brukerDataService;
    private final BrukerRepository brukerRepository;

    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final PersonId personId = PersonId.of("123");


    @Autowired
    public BrukerDataServiceTest(AktivitetDAO aktivitetDAO, JdbcTemplate jdbcTemplate, TiltakRepositoryV2 tiltakRepositoryV2, GruppeAktivitetRepository gruppeAktivitetRepository, BrukerDataRepository brukerDataRepository, BrukerService brukerService, BrukerRepository brukerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.brukerRepository = brukerRepository;
        brukerDataService = new BrukerDataService(aktivitetDAO, tiltakRepositoryV2, gruppeAktivitetRepository, brukerDataRepository, brukerService, mock(ElasticIndexer.class));
    }

    @BeforeEach
    public void reset() {
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGING_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTIVITETER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.BRUKER_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.BRUKERTILTAK_V2.TABLE_NAME);
        jdbcTemplate.execute("truncate table BRUKERSTATUS_AKTIVITETER");
    }

    @Test
    public void skalOppdatereBrukerData(){
        Timestamp enUkeSiden = Timestamp.valueOf(LocalDateTime.now().minusDays(7));
        Timestamp toUkerSiden = Timestamp.valueOf(LocalDateTime.now().minusDays(14));

        Timestamp enUkeTil = Timestamp.valueOf(LocalDateTime.now().plusDays(7));
        Timestamp toUkerTil = Timestamp.valueOf(LocalDateTime.now().plusDays(14));
        Timestamp treUkerTil = Timestamp.valueOf(LocalDateTime.now().plusDays(21));

        insertAktivitet(toUkerSiden, enUkeSiden);
        insertAktivitet(enUkeTil, toUkerTil);
        insertTiltak(toUkerTil, treUkerTil);

        brukerDataService.oppdaterAktivitetBrukerData(aktorId, personId);
        Brukerdata brukerdata = brukerRepository.retrieveBrukerdata(List.of(personId.getValue())).get(0);
        System.out.println(brukerdata);

        assertThat(brukerdata.getNyesteUtlopteAktivitet()).isEqualTo(enUkeSiden);

        assertThat(brukerdata.getAktivitetStart()).isEqualTo(enUkeTil);
        assertThat(brukerdata.getNesteAktivitetStart()).isEqualTo(toUkerTil);
        assertThat(brukerdata.getForrigeAktivitetStart()).isEqualTo(toUkerSiden);
    }

    private void insertTiltak(Timestamp startDato, Timestamp tilDato) {
        String id = String.valueOf(current().nextInt());
        SqlUtils.upsert(jdbcTemplate, Table.BRUKERTILTAK_V2.TABLE_NAME)
                .set(Table.BRUKERTILTAK_V2.AKTIVITETID, id)
                .set(Table.BRUKERTILTAK_V2.PERSONID, personId.getValue())
                .set(Table.BRUKERTILTAK_V2.AKTOERID, aktorId.get())
                .set(Table.BRUKERTILTAK_V2.TILTAKSKODE, "GRUPPEAMO")
                .set(Table.BRUKERTILTAK_V2.FRADATO, startDato)
                .set(Table.BRUKERTILTAK_V2.TILDATO, tilDato)
                .where(WhereClause.equals(Table.BRUKERTILTAK_V2.AKTIVITETID,  id))
                .execute();
    }

    private void insertAktivitet(Timestamp startDato, Timestamp tilDato) {
        String id = String.valueOf(current().nextInt());
        SqlUtils.upsert(jdbcTemplate, Table.AKTIVITETER.TABLE_NAME)
                .set(Table.AKTIVITETER.AKTOERID, aktorId.get())
                .set(Table.AKTIVITETER.AKTIVITETTYPE, "egen")
                .set(Table.AKTIVITETER.AVTALT, true)
                .set(Table.AKTIVITETER.FRADATO, startDato)
                .set(Table.AKTIVITETER.TILDATO, tilDato)
                .set(Table.AKTIVITETER.OPPDATERTDATO, Timestamp.valueOf(LocalDateTime.now()))
                .set(Table.AKTIVITETER.STATUS, "GJENNOMFORES".toLowerCase())
                .set(Table.AKTIVITETER.VERSION, 1)
                .set(Table.AKTIVITETER.AKTIVITETID, id)
                .where(WhereClause.equals(Table.AKTIVITETER.AKTIVITETID,id))
                .execute();
    }

    private void insertBruker() {
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, testEnhet.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktorId.toString())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.toString())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktorId.toString())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.toString())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();
    }
}
