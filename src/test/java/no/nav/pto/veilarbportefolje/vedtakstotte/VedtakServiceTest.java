package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static no.nav.common.json.JsonUtils.fromJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VedtakServiceTest {

    private VedtakStatusRepositoryV2 vedtakStatusRepository;
    private VedtakService vedtakService;
    private static final String AKTORID = "123456789";
    private static final long VEDTAKID = 1;
    private static final String VEILEDER_IDENT = "Z1234";
    private static final String VEILEDER_NAVN = "Veileder 1234";

    private static final KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
            .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.UTKAST_OPPRETTET)
            .setTimestamp(DateUtils.now().toLocalDateTime())
            .setAktorId(AKTORID)
            .setVedtakId(VEDTAKID)
            .setHovedmal(null)
            .setInnsatsgruppe(null)
            .setVeilederIdent(VEILEDER_IDENT)
            .setVeilederNavn(VEILEDER_NAVN);

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        this.vedtakStatusRepository = new VedtakStatusRepositoryV2(db);
        OpensearchIndexer opensearchIndexer = mock(OpensearchIndexer.class);
        this.vedtakService = new VedtakService(vedtakStatusRepository, opensearchIndexer);
        vedtakStatusRepository.slettGamleVedtakOgUtkast(AKTORID);
    }

    @Test
    public void skallSetteInUtkast() {
        vedtakService.behandleKafkaMeldingLogikk(vedtakStatusEndring);
        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hent14aVedtak(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
        assertThat(endringer.get()).isEqualTo(vedtakStatusEndring);
    }

    @Test
    public void skallOppdatereUtkast_sendtutkast() {
        vedtakService.behandleKafkaMeldingLogikk(vedtakStatusEndring);
        LocalDateTime time = DateUtils.now().toLocalDateTime();
        KafkaVedtakStatusEndring kafkaVedtakSendtTilBeslutter = new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.VEDTAK_SENDT)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        vedtakService.behandleKafkaMeldingLogikk(kafkaVedtakSendtTilBeslutter);

        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hent14aVedtak(AKTORID);
        assertThat(endringer.isEmpty()).isTrue();
    }

    @Test
    public void skallSletteGamleVedtak_sendtutkast() {
        vedtakStatusRepository.upsertVedtak(new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.VEDTAK_SENDT)
                .setTimestamp(DateUtils.now().toLocalDateTime())
                .setAktorId(AKTORID)
                .setVedtakId(2)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS));

        KafkaVedtakStatusEndring kafkaVedtakSendtTilBruker = new KafkaVedtakStatusEndring()
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.VEDTAK_SENDT)
                .setTimestamp(DateUtils.now().toLocalDateTime())
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        vedtakService.behandleKafkaMeldingLogikk(kafkaVedtakSendtTilBruker);

        Optional<KafkaVedtakStatusEndring> endringer = vedtakStatusRepository.hent14aVedtak(AKTORID);
        assertThat(endringer.isEmpty()).isTrue();
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
