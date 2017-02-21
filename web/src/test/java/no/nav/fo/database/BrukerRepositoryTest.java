package no.nav.fo.database;

import com.google.common.base.Joiner;
import no.nav.fo.config.ApplicationConfigTest;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationConfigTest.class})
public class BrukerRepositoryTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private BrukerRepository brukerRepository;

    @Before
    public void setUp() {
        try {
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/create-table-oppfolgingsbruker.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/create-table-metadata.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-metadata-sist-indeksert.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-test-data-indeksering_logg.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/create-table-aktoerid-to-personid-mapping.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-aktoerid-to-personid-testdata.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/create-table-bruker-data.sql"))));
            jdbcTemplate.execute(Joiner.on("\n").join(IOUtils.readLines(BrukerRepositoryTest.class.getResourceAsStream("/insert-bruker-data-test.sql"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("drop table oppfolgingsbruker");
        jdbcTemplate.execute("drop table metadata");
    }

    @Test
    public void skalHenteUtAlleBrukereFraDatabasen() {
        List<Map<String, Object>> brukere = jdbcTemplate.queryForList(brukerRepository.retrieveBrukereSQL());

        assertThat(brukere.size()).isEqualTo(72);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtAlleBrukere() {
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(brukerRepository.retrieveBrukereSQL()).get(0).keySet();
        String[] skalHaDatabaseFelter = new String[] {"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
                "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
                "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL"};

        assertThat(faktiskeDatabaseFelter).containsExactly(skalHaDatabaseFelter);
    }

    @Test
    public void skalHenteKunNyesteBrukereFraDatabasen() {
        List<Map<String, Object>> nyeBrukere = jdbcTemplate.queryForList(brukerRepository.retrieveOppdaterteBrukereSQL());
        jdbcTemplate.queryForList(brukerRepository.retrieveSistIndeksertSQL());
        assertThat(nyeBrukere.size()).isEqualTo(4);
    }

    @Test
    public void skalHaFolgendeFelterNaarHenterUtNyeBrukere() {
        Set<String> faktiskeDatabaseFelter = jdbcTemplate.queryForList(brukerRepository.retrieveOppdaterteBrukereSQL()).get(0).keySet();
        String[] skalHaDatabaseFelter = new String[] {"PERSON_ID", "FODSELSNR", "FORNAVN", "ETTERNAVN", "NAV_KONTOR",
                "FORMIDLINGSGRUPPEKODE", "ISERV_FRA_DATO", "KVALIFISERINGSGRUPPEKODE", "RETTIGHETSGRUPPEKODE",
                "HOVEDMAALKODE", "SIKKERHETSTILTAK_TYPE_KODE", "FR_KODE", "SPERRET_ANSATT", "ER_DOED", "DOED_FRA_DATO", "TIDSSTEMPEL"};

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
        List<Map<String,Object>> mapping = brukerRepository.retrievePersonid("11111111");
        String personid = (String) mapping.get(0).get("PERSONID");
        Assertions.assertThat(personid).isEqualTo("222222");
    }

    @Test
    public void skalOppdatereOmBrukerFinnes() {
        String aktoerid = (String) jdbcTemplate.queryForList("SELECT * FROM BRUKER_DATA").get(0).get("AKTOERID");
        brukerRepository.insertOrUpdateBrukerdata(aktoerid,"555555","X444444","2017-01-14 09:59:56.000000");
        String veilederident = (String) jdbcTemplate.queryForList("SELECT VEILEDERIDENT FROM BRUKER_DATA WHERE AKTOERID='111111'").get(0).get("VEILEDERIDENT");
        Assertions.assertThat(veilederident).isEqualTo("X444444");
    }

    @Test
    public void skalInserteOmBrukerIkkeFinnes() {
        String aktoerid = "999999"; //aktoerid som ikke finnes i databasen.
        List<Map<String,Object>> brukere = brukerRepository.retrieveBruker(aktoerid);
        Assertions.assertThat(brukere).isEmpty();

        brukerRepository.insertOrUpdateBrukerdata("999999","555555","X444444","2017-01-14 09:59:56.000000");
        String veilederident = (String) brukerRepository.retrieveBruker(aktoerid).get(0).get("VEILEDERIDENT");
        Assertions.assertThat(veilederident).isEqualTo("X444444");
    }
}
