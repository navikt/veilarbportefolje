package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class VedtaksStatusRepositoryTestv2 {

    private VedtakStatusRepositoryV2 vedtakStatusRepository;

    private static String AKTORID = "123456789";
    private static long VEDTAKID = 1;
    private static final String VEILEDER_IDENT = "Z1234";
    private static final String VEILEDER_NAVN = "Veileder 1234";

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        this.vedtakStatusRepository = new VedtakStatusRepositoryV2(db);
        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);
    }

    @Test
    public void skallSetteInVedtak() {
        insertVedtakIDB();
        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
    }

    @Test
    public void skallOppdatereVedtakMedAnsvarligVeileder() {
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.KLAR_TIL_BESLUTTER)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.STANDARD_INNSATS)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVeilederNavn(VEILEDER_NAVN)
                .setVeilederIdent(VEILEDER_IDENT)
                .setVedtakId(VEDTAKID);

        vedtakStatusRepository.upsertVedtak(vedtakStatusEndring);
        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
        assertThat(endringer.get().veilederIdent).isEqualTo(VEILEDER_IDENT);
        assertThat(endringer.get().veilederNavn).isEqualTo(VEILEDER_NAVN);
        assertThat(endringer.get().aktorId).isEqualTo(AKTORID);
        assertThat(endringer.get().vedtakId).isEqualTo(VEDTAKID);
        assertThat(endringer.get().getTimestamp()).isEqualTo(time);
    }


    private void insertVedtakIDB() {
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.UTKAST_OPPRETTET)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(null)
                .setInnsatsgruppe(null)
                .setVeilederNavn(VEILEDER_NAVN)
                .setVeilederIdent(VEILEDER_IDENT);
        vedtakStatusRepository.upsertVedtak(vedtakStatusEndring);
    }
}
