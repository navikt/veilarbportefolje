package no.nav.fo.database;

import io.vavr.control.Try;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.VeilederId;
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationConfigTest.class})
public class ArbeidslisteRepositoryTest {

    @Inject
    private ArbeidslisteRepository repo;

    @Inject
    private JdbcTemplate jdbcTemplate;

    private ArbeidslisteData data;
    private ArbeidslisteData data2;

    @Before
    public void setUp() throws Exception {
        data = new ArbeidslisteData(new Fnr("01010101010"))
                .setAktoerId(AktoerId.of("22222222"))
                .setVeilederId(VeilederId.of("X11111"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar");

        data2 = new ArbeidslisteData(new Fnr("01010101011"))
                .setAktoerId(AktoerId.of("22222223"))
                .setVeilederId(VeilederId.of("X11112"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar");

        jdbcTemplate.execute("TRUNCATE TABLE ARBEIDSLISTE");

        Try<AktoerId> result1 = repo.insertArbeidsliste(data);
        Try<AktoerId> result2 = repo.insertArbeidsliste(data2);
        assertTrue(result1.isSuccess());
    }

    @Test
    public void skalKunneHenteArbeidsliste() throws Exception {
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktoerId());
        assertTrue(result.isSuccess());
        assertEquals(data.getVeilederId(), result.get().getSistEndretAv());
    }

    @Test
    public void skalOppdatereEksisterendeArbeidsliste() throws Exception {
        VeilederId expected = VeilederId.of("TEST_ID");
        repo.updateArbeidsliste(data.setVeilederId(expected));

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktoerId());
        assertTrue(result.isSuccess());
        assertEquals(expected, result.get().getSistEndretAv());
    }

    @Test
    public void skalSletteEksisterendeArbeidsliste() throws Exception {
        Try<AktoerId> result = repo.deleteArbeidsliste(data.getAktoerId());
        assertTrue(result.isSuccess());
    }

    @Test
    public void skalReturnereFailureVedFeil() throws Exception {
        Try<AktoerId> result = repo.insertArbeidsliste(data.setAktoerId(null));
        assertTrue(result.isFailure());
    }

    @Test
    public void skalHenteArbeidslisteForListeAvAktoerid() {
        AktoerId aktoerId1 = AktoerId.of("22222222");
        AktoerId aktoerId2 = AktoerId.of("22222223");
        AktoerId aktoerId3 = AktoerId.of("finnesikke");
        Map<AktoerId, Optional<Arbeidsliste>> arbeidslisteMap = repo.retrieveArbeidsliste(asList(aktoerId1,aktoerId2, aktoerId3));
        assertTrue(arbeidslisteMap.get(aktoerId1).isPresent());
        assertTrue(arbeidslisteMap.get(aktoerId2).isPresent());
        assertFalse(arbeidslisteMap.get(aktoerId3).isPresent());
    }

    @Test
    public void skalSletteArbeidslisteForAktoerids() {
        AktoerId aktoerId1 = AktoerId.of("22222222");
        AktoerId aktoerId2 = AktoerId.of("22222223");
        Map<AktoerId, Optional<Arbeidsliste>> arbeidsliste = repo.retrieveArbeidsliste(asList(aktoerId1,aktoerId2));
        assertTrue(arbeidsliste.get(aktoerId1).isPresent());
        assertTrue(arbeidsliste.get(aktoerId2).isPresent());

        repo.deleteArbeidslisteForAktoerids(asList(aktoerId1,aktoerId2));
        Map<AktoerId, Optional<Arbeidsliste>> arbeidslisteDeleted = repo.retrieveArbeidsliste(asList(aktoerId1,aktoerId2));
        assertFalse(arbeidslisteDeleted.get(aktoerId1).isPresent());
        assertFalse(arbeidslisteDeleted.get(aktoerId2).isPresent());
    }

    @Test
    public void skalIkkeTryneOmListeErTom() {
        repo.deleteArbeidslisteForAktoerids(Collections.emptyList());
    }
}