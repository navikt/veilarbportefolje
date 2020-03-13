package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.AktoerServiceMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VedtakServiceTest {

    private VedtakStatusRepository vedtakStatusRepository;
    private VedtakService vedtakService;
    private static String AKTORID = "123456789";
    private static long VEDTAKID = 1;

    private static KafkaVedtakStatusEndring kafkaVedtakStatusEndring = new KafkaVedtakStatusEndring()
            .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.UTKAST_OPPRETTET)
            .setStatusEndretTidspunkt(LocalDateTime.now())
            .setAktorId(AKTORID)
            .setVedtakId(VEDTAKID)
            .setHovedmal(null)
            .setInnsatsgruppe(null);

    @Before
    public void setup (){
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.vedtakStatusRepository = new VedtakStatusRepository(db);
        this.vedtakService = new VedtakService(vedtakStatusRepository, mock(ElasticIndexer.class), new AktoerServiceMock());

        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);

    }

    @Test
    public void skallSetteInUtkast()  {
        vedtakService.behandleMelding(kafkaVedtakStatusEndring);
        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakStatusEndring);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallOppdatereUtkast()  {
        vedtakService.behandleMelding(kafkaVedtakStatusEndring);
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring kafkaVedtakSendtTilBeslutter = new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BESLUTTER)
                .setStatusEndretTidspunkt(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);
        vedtakService.behandleMelding(kafkaVedtakSendtTilBeslutter);

        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakSendtTilBeslutter);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallSletteGamleVedtak()  {
        vedtakStatusRepository.upsertVedtak(new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BRUKER)
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setAktorId(AKTORID)
                .setVedtakId(2)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS));

        KafkaVedtakStatusEndring kafkaVedtakSendtTilBruker = new KafkaVedtakStatusEndring()
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BRUKER)
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS);


        vedtakService.behandleMelding(kafkaVedtakSendtTilBruker);

        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakSendtTilBruker);
        assertThat(endringer.size()).isEqualTo(1);

    }
}
