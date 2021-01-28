package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ArbeidslisteRepositoryTest {
    private ArbeidslisteRepository repo;
    private ArbeidslisteDTO data;

    @Before
    public void setUp() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(TestUtil.setupInMemoryDatabase());

        repo = new ArbeidslisteRepository(jdbcTemplate);

        data = new ArbeidslisteDTO(new Fnr("01010101010"))
                .setAktorId(AktorId.of("22222222"))
                .setVeilederId(VeilederId.of("X11111"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);

        ArbeidslisteDTO data2 = new ArbeidslisteDTO(new Fnr("01010101011"))
                .setAktorId(AktorId.of("22222223"))
                .setVeilederId(VeilederId.of("X11112"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar")
                .setKategori(Arbeidsliste.Kategori.GRONN);

        jdbcTemplate.execute("TRUNCATE TABLE ARBEIDSLISTE");

        Try<ArbeidslisteDTO> result1 = repo.insertArbeidsliste(data);
        Try<ArbeidslisteDTO> result2 = repo.insertArbeidsliste(data2);
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
    }

    @Test
    public void skalKunneHenteArbeidsliste() {
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktorId());
        assertThat(result.isSuccess()).isTrue();
        assertThat(data.getVeilederId()).isEqualTo(result.get().getSistEndretAv());
    }

    @Test
    public void skalKunneOppdatereKategori() {
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktorId());
        assertThat(Arbeidsliste.Kategori.BLA).isEqualTo(result.get().getKategori());

        Try<Arbeidsliste> updatedArbeidsliste = result
                .map(arbeidsliste -> new ArbeidslisteDTO(Fnr.of("01010101010"))
                        .setAktorId(data.getAktorId())
                        .setVeilederId(data.getVeilederId())
                        .setEndringstidspunkt(data.getEndringstidspunkt())
                        .setFrist(data.getFrist())
                        .setKommentar(data.getKommentar())
                        .setKategori(Arbeidsliste.Kategori.LILLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getAktorId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(Arbeidsliste.Kategori.LILLA).isEqualTo(updatedArbeidsliste.get().getKategori());
    }


    @Test
    public void skalOppdatereEksisterendeArbeidsliste() {
        VeilederId expected = VeilederId.of("TEST_ID");
        repo.updateArbeidsliste(data.setVeilederId(expected));

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktorId());

        assertThat(result.isSuccess()).isTrue();
        assertThat(expected).isEqualTo(result.get().getSistEndretAv());
    }

    @Test
    public void skalSletteEksisterendeArbeidsliste() {
        final Integer rowsUpdated = repo.slettArbeidsliste(data.getAktorId());
        assertThat(rowsUpdated);
    }

    @Test
    public void skalReturnereFailureVedFeil() {
        Try<ArbeidslisteDTO> result = repo.insertArbeidsliste(data.setAktorId(null));
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    public void skalSletteArbeidslisteForAktoerids() {
        AktorId aktoerId1 = AktorId.of("22222222");
        Try<Arbeidsliste> arbeidsliste = repo.retrieveArbeidsliste(aktoerId1);
        assertThat(arbeidsliste.isSuccess()).isTrue();
        assertThat(arbeidsliste.get()).isNotNull();

        final Integer rowsUpdated = repo.slettArbeidsliste(aktoerId1);
        assertThat(rowsUpdated).isEqualTo(1);

        arbeidsliste = repo.retrieveArbeidsliste(aktoerId1);
        assertThat(arbeidsliste.isSuccess()).isTrue();
        assertThat(arbeidsliste.get()).isNull();
    }

}
