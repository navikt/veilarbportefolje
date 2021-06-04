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
    public void skallOppretteVedtak() {
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

    @Test
    public void skallOppdatereVedtakMedAnsvarligVeileder() {
        insertVedtakIDB();

        Optional<KafkaVedtakStatusEndring> endringer1 = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer1.isPresent()).isTrue();
        assertThat(endringer1.get().veilederIdent).isEqualTo(null);
        assertThat(endringer1.get().veilederNavn).isEqualTo(null);

        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setVeilederNavn(VEILEDER_NAVN)
                .setVeilederIdent(VEILEDER_IDENT);

        vedtakStatusRepository.oppdaterAnsvarligVeileder(vedtakStatusEndring);

        Optional<KafkaVedtakStatusEndring> endringer2 = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer2.isPresent()).isTrue();
        assertThat(endringer2.get().veilederIdent).isEqualTo(VEILEDER_IDENT);
        assertThat(endringer2.get().veilederNavn).isEqualTo(VEILEDER_NAVN);
    }

    @Test
    public void skallSletteVedtak() {
        insertVedtakIDB();
        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);
        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.isEmpty()).isTrue();
    }
    @Test
    public void skallOppdatereVedtak() {
        insertVedtakIDB();

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

        vedtakStatusRepository.updateVedtak(vedtakStatusEndring);
        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
        assertThat(endringer.get().veilederIdent).isEqualTo(null); //TODO: finn ut om dette er riktig.
        assertThat(endringer.get().veilederNavn).isEqualTo(null);  // her som i den "gamle" versjonen oppdaters dette og forrige felt kun i funksjonen oppdaterAnsvarligVeileder()
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
                .setVeilederNavn(null)
                .setVeilederIdent(null);
        vedtakStatusRepository.upsertVedtak(vedtakStatusEndring);
    }
}
