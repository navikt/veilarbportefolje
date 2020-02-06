package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepository;
import no.nav.pto.veilarbportefolje.domene.Hovedmal;
import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class VedtakServiceTest {

    private VedtakStatusRepository vedtakStatusRepository;
    private VedtakService vedtakService;
    private static String AKTORID = "123456789";
    private static long VEDTAKID = 1;

    @Before
    public void setup (){
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.vedtakStatusRepository = new VedtakStatusRepository(db);
        this.vedtakService = new VedtakService(vedtakStatusRepository);

    }

    @Test
    public void skallSetteInUtkast()  {
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring kafkaVedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.UTKAST_OPPRETTET)
                .setStatusEndretTidspunkt(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(null)
                .setInnsatsgruppe(null);
        vedtakService.behandleMelding(kafkaVedtakStatusEndring);

       List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
       assertThat(endringer.get(0)).isEqualTo(kafkaVedtakStatusEndring);
       assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallOppdatereUtkast()  {
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring kafkaVedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BESLUTTER)
                .setStatusEndretTidspunkt(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(Hovedmal.BEHOLDEA)
                .setInnsatsgruppe(Innsatsgruppe.BATT);
        vedtakService.behandleMelding(kafkaVedtakStatusEndring);

        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakStatusEndring);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallSletteGamleVedtak()  {
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring kafkaVedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BRUKER)
                .setStatusEndretTidspunkt(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(Hovedmal.SKAFFEA)
                .setInnsatsgruppe(Innsatsgruppe.VARIG);
        vedtakService.behandleMelding(kafkaVedtakStatusEndring);

        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakStatusEndring);
        assertThat(endringer.size()).isEqualTo(1);

    }
}
