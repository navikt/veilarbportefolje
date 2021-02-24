package no.nav.pto.veilarbportefolje.vedtakstotte;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;

public class VedtaksStatusRepositoryTest {

    private VedtakStatusRepository vedtakStatusRepository;

    private static String AKTORID = "123456789";
    private static long VEDTAKID = 1;
    private static final String VEILEDER_IDENT = "Z1234";
    private static final String VEILEDER_NAVN = "Veileder 1234";

    @Before
    public void setup() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.vedtakStatusRepository = new VedtakStatusRepository(db);
        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);
    }

    @Test
    public void skallSetteInVedtak() {
        insertVedtakIDB();
        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.size()).isEqualTo(1);
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

        vedtakStatusRepository.upsertVedtakMedAnsvarligVeileder(vedtakStatusEndring);
        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.size()).isEqualTo(1);
        assertThat(endringer.get(0).veilederIdent).isEqualTo(VEILEDER_IDENT);
        assertThat(endringer.get(0).veilederNavn).isEqualTo(VEILEDER_NAVN);
        assertThat(endringer.get(0).aktorId).isEqualTo(AKTORID);
        assertThat(endringer.get(0).vedtakId).isEqualTo(VEDTAKID);
        assertThat(endringer.get(0).getTimestamp()).isEqualTo(time);
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
        vedtakStatusRepository.upsertVedtakMedAnsvarligVeileder(vedtakStatusEndring);
    }

}
