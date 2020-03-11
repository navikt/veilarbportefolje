package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepository;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationConfigTest.class})
public class ArbeidslisteRepositoryTest {

    @Inject
    private ArbeidslisteRepository repo;

    @Inject
    private JdbcTemplate jdbcTemplate;

    private ArbeidslisteDTO data;
    private ArbeidslisteDTO data2;

    @Before
    public void setUp() throws Exception {
        data = new ArbeidslisteDTO(new Fnr("01010101010"))
                .setAktoerId(AktoerId.of("22222222"))
                .setVeilederId(VeilederId.of("X11111"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);

        data2 = new ArbeidslisteDTO(new Fnr("01010101011"))
                .setAktoerId(AktoerId.of("22222223"))
                .setVeilederId(VeilederId.of("X11112"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar")
                .setKategori(Arbeidsliste.Kategori.GRONN);

        jdbcTemplate.execute("TRUNCATE TABLE ARBEIDSLISTE");

        Try<AktoerId> result1 = repo.insertArbeidsliste(data);
        Try<AktoerId> result2 = repo.insertArbeidsliste(data2);
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
    }

    @Test
    public void skalKunneHenteArbeidsliste() throws Exception {
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktoerId());
        assertTrue(result.isSuccess());
        assertEquals(data.getVeilederId(), result.get().getSistEndretAv());
    }

    @Test
    public void skalKunneOppdatereKategori() throws Exception {
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktoerId());
        assertEquals(Arbeidsliste.Kategori.BLA, result.get().getKategori());

        Try<Arbeidsliste> updatedArbeidsliste = result
                .map(arbeidsliste -> new ArbeidslisteDTO(Fnr.of("01010101010"))
                        .setAktoerId(data.getAktoerId())
                        .setVeilederId(data.getVeilederId())
                        .setEndringstidspunkt(data.getEndringstidspunkt())
                        .setFrist(data.getFrist())
                        .setKommentar(data.getKommentar())
                        .setKategori(Arbeidsliste.Kategori.LILLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(aktoerId -> repo.retrieveArbeidsliste(aktoerId));

        assertTrue(result.isSuccess());
        assertEquals(Arbeidsliste.Kategori.LILLA, updatedArbeidsliste.get().getKategori());
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
    public void skalSletteArbeidslisteForAktoerids() {
        AktoerId aktoerId1 = AktoerId.of("22222222");
        Try<Arbeidsliste> arbeidsliste = repo.retrieveArbeidsliste(aktoerId1);
        assertTrue(arbeidsliste.isSuccess());
        assertTrue(arbeidsliste.get() != null);

        repo.deleteArbeidslisteForAktoerid(aktoerId1);
        arbeidsliste = repo.retrieveArbeidsliste(aktoerId1);
        assertTrue(arbeidsliste.isSuccess());
        assertFalse(arbeidsliste.get() != null);
    }

}
