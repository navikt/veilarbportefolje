package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VedtakServiceTest {

    private VedtakStatusRepository vedtakStatusRepository;
    private VedtakService vedtakService;
    private static final String AKTORID = "123456789";
    private static final long VEDTAKID = 1;

    private static final KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
            .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.UTKAST_OPPRETTET)
            .setTimestamp(LocalDateTime.now())
            .setAktorId(AKTORID)
            .setVedtakId(VEDTAKID)
            .setHovedmal(null)
            .setInnsatsgruppe(null);

    @Before
    public void setup (){
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.vedtakStatusRepository = new VedtakStatusRepository(db);
        ElasticIndexer elasticIndexer = mock(ElasticIndexer.class);
        this.vedtakService = new VedtakService(vedtakStatusRepository, elasticIndexer, mock(BrukerService.class));
        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);
    }

    @Test
    public void skallSetteInUtkast()  {
        vedtakService.behandleKafkaMelding(toJson(vedtakStatusEndring));
        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(vedtakStatusEndring);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallOppdatereUtkast()  {
        vedtakService.behandleKafkaMelding(toJson(vedtakStatusEndring));
        LocalDateTime time = LocalDateTime.now();
        KafkaVedtakStatusEndring kafkaVedtakSendtTilBeslutter = new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.VEDTAK_SENDT)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        vedtakService.behandleKafkaMelding(toJson(kafkaVedtakSendtTilBeslutter));

        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakSendtTilBeslutter);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallSletteGamleVedtak()  {
        vedtakStatusRepository.upsertVedtak(new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.VEDTAK_SENDT)
                .setTimestamp(LocalDateTime.now())
                .setAktorId(AKTORID)
                .setVedtakId(2)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS));

        KafkaVedtakStatusEndring kafkaVedtakSendtTilBruker = new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.VEDTAK_SENDT)
                .setTimestamp(LocalDateTime.now())
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS);


        vedtakService.behandleKafkaMelding(toJson(kafkaVedtakSendtTilBruker));

        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(kafkaVedtakSendtTilBruker);
        assertThat(endringer.size()).isEqualTo(1);

    }
}
