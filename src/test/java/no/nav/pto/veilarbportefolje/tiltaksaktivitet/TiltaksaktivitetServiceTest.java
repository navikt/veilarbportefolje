package no.nav.pto.veilarbportefolje.tiltaksaktivitet;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.dto.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.aktiviteter.dto.KafkaAktivitetMelding.AktivitetTypeData.TILTAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TiltaksaktivitetServiceTest extends EndToEndTest {

    private TiltaksaktivitetRepository tiltaksaktivitetRepository;

    private TiltaksaktivitetService tiltaksaktivitetService;

    private JdbcTemplate jdbcTemplatePostgres;

    @Autowired
    public TiltaksaktivitetServiceTest(JdbcTemplate jdbcTemplatePostgres, TiltaksaktivitetRepository tiltaksaktivitetRepository, TiltaksaktivitetService tiltaksaktivitetService) {
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.tiltaksaktivitetRepository = tiltaksaktivitetRepository;
        this.tiltaksaktivitetService = tiltaksaktivitetService;
    }

    @BeforeEach
    void setUp() {
        tiltaksaktivitetRepository = mock(TiltaksaktivitetRepository.class);
        tiltaksaktivitetService = new TiltaksaktivitetService(tiltaksaktivitetRepository);

        jdbcTemplatePostgres.update("TRUNCATE TABLE TILTAKSAKTIVITET");
        jdbcTemplatePostgres.update("TRUNCATE TABLE TILTAKSKODEVERK");
    }

    @Test
    void skal_ignorere_null_melding() {
        tiltaksaktivitetService.behandleKafkaMeldingLogikk(null);
        verifyNoInteractions(tiltaksaktivitetRepository);
    }

    @Test
    void skal_ignorere_melding_mangler_aktivitetId() {
        KafkaAktivitetMelding melding = new KafkaAktivitetMelding()
                .setAktivitetType(TILTAK);

        tiltaksaktivitetService.behandleKafkaMeldingLogikk(melding);
        verifyNoInteractions(tiltaksaktivitetRepository);
    }

    @Test
    void skal_slette_naar_historisk_er_true() {
        KafkaAktivitetMelding melding = new KafkaAktivitetMelding()
                .setAktivitetId("AktivitetId-1")
                .setAktivitetType(TILTAK)
                .setTiltakskode("T123")
                .setAktorId("123")
                .setHistorisk(true);

        tiltaksaktivitetService.behandleKafkaMeldingLogikk(melding);

        verify(tiltaksaktivitetRepository).deleteTiltaksaktivitet("AktivitetId-1");
        verifyNoMoreInteractions(tiltaksaktivitetRepository);
    }

    @Test
    void skal_lagre_naar_ny_versjon_er_storre_eller_lik() {
        KafkaAktivitetMelding melding = new KafkaAktivitetMelding()
                .setAktivitetId("Aktivitet-2")
                .setAktivitetType(TILTAK)
                .setTiltakskode("SOMMERJOBB")
                .setAktorId("AktorId-1")
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now().plusDays(10))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(5L)
                .setHistorisk(false);

        when(tiltaksaktivitetRepository.hentSistVersjonAvAktivitet("Aktivitet-2")).thenReturn(3L);

        tiltaksaktivitetService.behandleKafkaMeldingLogikk(melding);

        ArgumentCaptor<TiltakaktivitetEntity> captor = ArgumentCaptor.forClass(TiltakaktivitetEntity.class);
        verify(tiltaksaktivitetRepository).upsert(captor.capture(), eq(AktorId.of("AktorId-1")));

        TiltakaktivitetEntity entity = captor.getValue();
        assertThat(entity.getAktivitetId()).isEqualTo("Aktivitet-2");
        assertThat(entity.getTiltakskode()).isEqualTo("SOMMERJOBB");
    }

    @Test
    void skal_ikke_lagre_naar_kommende_version_er_mindre_enn_database() {
        KafkaAktivitetMelding melding = new KafkaAktivitetMelding()
                .setAktivitetId("AktivitetId-3")
                .setAktivitetType(TILTAK)
                .setTiltakskode("SOMMERJOBB")
                .setAktorId("AktorId-2")
                .setVersion(2L);

        when(tiltaksaktivitetRepository.hentSistVersjonAvAktivitet("AktivitetId-3")).thenReturn(10L);

        tiltaksaktivitetService.behandleKafkaMeldingLogikk(melding);

        verify(tiltaksaktivitetRepository, never()).upsert(any(), any());
    }
}
