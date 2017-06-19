package no.nav.fo.database;

import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationConfigTest.class})
public class ArbeidslisteRepositoryTest {

    @Inject
    private ArbeidslisteRepository repo;

    @Inject
    private JdbcTemplate jdbcTemplate;

    private ArbeidslisteData data;

    @Before
    public void setUp() throws Exception {
        data = new ArbeidslisteData(new Fnr("01010101010"))
                .setAktoerID("22222222")
                .setVeilederId("X11111")
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar");

        jdbcTemplate.execute("TRUNCATE TABLE ARBEIDSLISTE");

        Optional<ArbeidslisteData> result = repo.insertArbeidsliste(data);
        assertTrue(result.isPresent());
    }

    @Test
    public void skalKunneHenteArbeidsliste() throws Exception {
        Optional<Arbeidsliste> resultAfterRetrieval = repo.retrieveArbeidsliste(data.getAktoerID());
        assertTrue(resultAfterRetrieval.isPresent());
        assertEquals(data.getVeilederId(), resultAfterRetrieval.get().getVeilederId());
    }

    @Test
    public void skalOppdatereEksisterendeArbeidsliste() throws Exception {
        String expected = "TEST_ID";
        repo.updateArbeidsliste(data.setVeilederId(expected));

        Optional<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktoerID());
        assertTrue(result.isPresent());
        assertEquals(expected, result.get().getVeilederId());
    }

    @Test
    public void skalSletteEksisterendeArbeidsliste() throws Exception {
        Optional<String> result = repo.deleteArbeidsliste(data.getAktoerID());
        assertTrue(result.isPresent());
    }

    @Test
    public void skalReturnereIngentingVedFeil() throws Exception {
        Optional<ArbeidslisteData> result = repo.insertArbeidsliste(data);
        assertFalse(result.isPresent());
    }
}