package no.nav.pto.veilarbportefolje.database;

import no.nav.pto.veilarbportefolje.domene.Hovedmal;
import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;

public class VedtaksStatusRepositoryTest {

    private VedtakStatusRepository vedtakStatusRepository;
    private JdbcTemplate db;


    @Before
    public void setup() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        vedtakStatusRepository = new VedtakStatusRepository(db);
    }

    @Test
    public void insertVedtakUtkast() {
        LocalDateTime time = LocalDateTime.now();
        String aktorId = "123456789";
        KafkaVedtakStatusEndring kafkaVedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.UTKAST_OPPRETTET)
                .setStatusEndretTidspunkt(time)
                .setAktorId(aktorId)
                .setId(0)
                .setSistRedigertTidspunkt(time);
        vedtakStatusRepository.upsertVedtak(kafkaVedtakStatusEndring);
        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(aktorId);

        assertThat(endringer.size()).isEqualTo(1);


    }



}
