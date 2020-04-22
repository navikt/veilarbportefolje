package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.AktoerServiceMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.json.JsonUtils.toJson;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VedtakServiceTest {

    private VedtakStatusRepository vedtakStatusRepository;
    private VedtakService vedtakService;
    private static String AKTORID = "123456789";
    private static long VEDTAKID = 1;

    private static VedtakStatusEndring vedtakStatusEndring = new VedtakStatusEndring()
            .setVedtakStatus(VedtakStatusEndring.KafkaVedtakStatus.UTKAST_OPPRETTET)
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
        vedtakService.behandleKafkaMelding(toJson(vedtakStatusEndring));
        List<VedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(vedtakStatusEndring);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallOppdatereUtkast()  {
        vedtakService.behandleKafkaMelding(toJson(vedtakStatusEndring));
        LocalDateTime time = LocalDateTime.now();
        VedtakStatusEndring kafkaVedtakSendtTilBeslutter = new VedtakStatusEndring()
                .setVedtakStatus(VedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BESLUTTER)
                .setStatusEndretTidspunkt(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(VedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID)
                .setInnsatsgruppe(VedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        vedtakService.behandleKafkaMelding(toJson(kafkaVedtakSendtTilBeslutter));

        List<VedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakSendtTilBeslutter);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallSletteGamleVedtak()  {
        vedtakStatusRepository.upsertVedtak(new VedtakStatusEndring()
                .setVedtakStatus(VedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BRUKER)
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setAktorId(AKTORID)
                .setVedtakId(2)
                .setHovedmal(VedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(VedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS));

        VedtakStatusEndring kafkaVedtakSendtTilBruker = new VedtakStatusEndring()
                .setVedtakStatus(VedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BRUKER)
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(VedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(VedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS);


        vedtakService.behandleKafkaMelding(toJson(kafkaVedtakSendtTilBruker));

        List<VedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakSendtTilBruker);
        assertThat(endringer.size()).isEqualTo(1);

    }
}
