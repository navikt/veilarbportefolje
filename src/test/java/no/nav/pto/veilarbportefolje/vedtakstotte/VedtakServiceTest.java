package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VedtakServiceTest {

    private VedtakStatusRepository vedtakStatusRepository;
    private VedtakService vedtakService;
    private static final String AKTORID = "123456789";
    private static final long VEDTAKID = 1;
    private static final String VEILEDER_IDENT = "Z1234";
    private static final String VEILEDER_NAVN = "Veileder 1234";

    private static final KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
            .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.UTKAST_OPPRETTET)
            .setTimestamp(LocalDateTime.now())
            .setAktorId(AKTORID)
            .setVedtakId(VEDTAKID)
            .setHovedmal(null)
            .setInnsatsgruppe(null)
            .setVeilederIdent(VEILEDER_IDENT)
            .setVeilederNavn(VEILEDER_NAVN);

    @Before
    public void setup() {
        UnleashService mock = mock(UnleashService.class);
        when(mock.isEnabled(anyString())).thenReturn(true);

        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.vedtakStatusRepository = new VedtakStatusRepository(db);
        ElasticIndexer elasticIndexer = mock(ElasticIndexer.class);
        VedtakStatusRepositoryV2 vedtakStatusRepositoryV2 = mock(VedtakStatusRepositoryV2.class);
        this.vedtakService = new VedtakService(vedtakStatusRepository, vedtakStatusRepositoryV2, elasticIndexer, mock);
        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);
    }

    @Test
    public void skallSetteInUtkast() {
        vedtakService.behandleKafkaMelding(toJson(vedtakStatusEndring));
        List<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hentVedtak(AKTORID);
        assertThat(endringer.get(0)).isEqualTo(vedtakStatusEndring);
        assertThat(endringer.size()).isEqualTo(1);
    }

    @Test
    public void skallOppdatereUtkast() {
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

    @Test
    public void testJsonDesrializationForVeilederInfo() {
        String inputJsonWithoutVeilederInfo = "{\"vedtakId\":1,\"aktorId\":\"1\",\"vedtakStatusEndring\":\"UTKAST_OPPRETTET\",\"timestamp\":\"2021-02-09T22:24:12.373356+01:00\"}";
        KafkaVedtakStatusEndring kafkaVedtakStatusEndring = fromJson(inputJsonWithoutVeilederInfo, KafkaVedtakStatusEndring.class);

        assertThat(kafkaVedtakStatusEndring.aktorId).isEqualTo("1");
        assertThat(kafkaVedtakStatusEndring.veilederIdent).isNull();
        assertThat(kafkaVedtakStatusEndring.veilederNavn).isNull();

        String inputJsonWithVeilederInfo = "{\"vedtakId\":1,\"aktorId\":\"1\",\"vedtakStatusEndring\":\"UTKAST_OPPRETTET\",\"timestamp\":\"2021-02-09T22:24:12.373356+01:00\", \"veilederIdent\":\"Z1234\", \"veilederNavn\":\"Test123\"}";
        kafkaVedtakStatusEndring = fromJson(inputJsonWithVeilederInfo, KafkaVedtakStatusEndring.class);
        assertThat(kafkaVedtakStatusEndring.veilederNavn).isEqualTo("Test123");
        assertThat(kafkaVedtakStatusEndring.veilederIdent).isEqualTo("Z1234");

    }
}
