package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.Assert.*;

public class ArbeidslisteRepositoryTest {

    private ArbeidslisteRepository repo;

    private ArbeidslisteDTO data;
    private ArbeidslisteDTO data2;

    @Before
    public void setUp() {
        DataSource dataSource = TestUtil.setupInMemoryDatabase();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        repo = new ArbeidslisteRepository(jdbcTemplate, new NamedParameterJdbcTemplate(dataSource));

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

        Try<ArbeidslisteDTO> result1 = repo.insertArbeidsliste(data);
        Try<ArbeidslisteDTO> result2 = repo.insertArbeidsliste(data2);
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
    }

    @Test
    public void skalKunneHenteArbeidsliste() throws Exception {
        Try<Arbeidsliste> result = repo.retrieveArbeidslisteFromDb(data.getAktoerId());
        assertTrue(result.isSuccess());
        assertEquals(data.getVeilederId(), result.get().getSistEndretAv());
    }

    @Test
    public void skalKunneOppdatereKategori() throws Exception {
        Try<Arbeidsliste> result = repo.retrieveArbeidslisteFromDb(data.getAktoerId());
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
                .flatMap(aktoerId -> repo.retrieveArbeidslisteFromDb(aktoerId.aktoerId));

        assertTrue(result.isSuccess());
        assertEquals(Arbeidsliste.Kategori.LILLA, updatedArbeidsliste.get().getKategori());
    }


    @Test
    public void skalOppdatereEksisterendeArbeidsliste() throws Exception {
        VeilederId expected = VeilederId.of("TEST_ID");
        repo.updateArbeidsliste(data.setVeilederId(expected));

        Try<Arbeidsliste> result = repo.retrieveArbeidslisteFromDb(data.getAktoerId());
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
        Try<ArbeidslisteDTO> result = repo.insertArbeidsliste(data.setAktoerId(null));
        assertTrue(result.isFailure());
    }

    @Test
    public void skalSletteArbeidslisteForAktoerids() {
        AktoerId aktoerId1 = AktoerId.of("22222222");
        Try<Arbeidsliste> arbeidsliste = repo.retrieveArbeidslisteFromDb(aktoerId1);
        assertTrue(arbeidsliste.isSuccess());
        assertTrue(arbeidsliste.get() != null);

        repo.deleteArbeidslisteForAktoerid(aktoerId1);
        arbeidsliste = repo.retrieveArbeidslisteFromDb(aktoerId1);
        assertTrue(arbeidsliste.isSuccess());
        assertFalse(arbeidsliste.get() != null);
    }

}
