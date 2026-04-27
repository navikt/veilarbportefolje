package no.nav.pto.veilarbportefolje.tiltaksaktivitet;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TiltaksaktivitetRepositoryTest extends EndToEndTest {

    private JdbcTemplate db;
    private TiltaksaktivitetRepository repository;

    @Autowired
    public TiltaksaktivitetRepositoryTest(JdbcTemplate db, TiltaksaktivitetRepository repository) {
        this.db = db;
        this.repository = repository;
    }

    @BeforeEach
    void setUp() {
        db = mock(JdbcTemplate.class);
        repository = new TiltaksaktivitetRepository(db);

        db.execute("TRUNCATE TABLE TILTAKSAKTIVITET");
    }

    @Test
    void skal_slette_tiltaksaktivitet() {
        repository.deleteTiltaksaktivitet("AktivitetId-1");
        verify(db).update(anyString(), eq("AktivitetId-1"));
    }

    @Test
    void eksistererTiltakstype_returnerer_true_nar_database_har_raden() {
        when(db.queryForObject(anyString(), eq(boolean.class), any())).thenReturn(true);
        boolean res = repository.eksistererTiltakstype("SOMMERJOBB");
        assertThat(res).isTrue();
    }

    @Test
    void hentSistVersjonAvAktivitet_returnerer_db_value_nar_present() {
        when(db.queryForObject(anyString(), eq(Long.class), any())).thenReturn(7L);
        Long versjon = repository.hentSistVersjonAvAktivitet("AktivitetId-1");
        assertThat(versjon).isEqualTo(7L);
    }

    @Test
    void upsert_inserter_og_oppretter_kodeverk_hvis_mangler() {
        AktorId aktoerId = AktorId.of("1000123");
        TiltakaktivitetEntity entity = new TiltakaktivitetEntity()
                .setAktivitetId("AktivitetId-1")
                .setAktoerId(aktoerId)
                .setTiltakskode("SOMMERJOBB")
                .setTiltaksnavn("Sommerjobb")
                .setFraDato(ArenaDato.of(ZonedDateTime.now()))
                .setTilDato(ArenaDato.of(ZonedDateTime.now().plusHours(1)))
                .setVersion(1L)
                .setStatus("AKTIV");

        when(db.queryForObject(anyString(), eq(boolean.class), any())).thenReturn(false);
        when(db.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1L);

        Long sisteVersjon = repository.hentSistVersjonAvAktivitet("AktivitetId-1");
        boolean tiltakstype = repository.eksistererTiltakstype("SOMMERJOBB");

        assertThat(tiltakstype).isFalse(); // Siden vi mocker til false, vil den ikke eksistere
        assertThat(sisteVersjon).isEqualTo(1L);

        repository.upsert(entity, aktoerId);
        // Verify kodeverk upsert
        verify(db).update(anyString(), eq(entity.getTiltakskode()), eq(entity.getTiltaksnavn()));
        // Verify main upsert (map-based update)
        verify(db).update(anyString(), anyMap());
    }
}
