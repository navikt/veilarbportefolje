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

public class Utkast14aStatusendringServiceeTest {

    private Utkast14aStatusRepository vedtakStatusRepository;
    private Utkast14aStatusendringService utkast14aStatusendringService;
    private static final String AKTORID = "123456789";
    private static final long VEDTAKID = 1;
    private static final String VEILEDER_IDENT = "Z1234";
    private static final String VEILEDER_NAVN = "Veileder 1234";

    private static final Kafka14aStatusendring vedtakStatusEndring = new Kafka14aStatusendring()
            .setVedtakStatusEndring(Kafka14aStatusendring.Status.UTKAST_OPPRETTET)
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
        this.vedtakStatusRepository = new Utkast14aStatusRepository(db);
        OpensearchIndexer opensearchIndexer = mock(OpensearchIndexer.class);
        this.utkast14aStatusendringService = new Utkast14aStatusendringService(vedtakStatusRepository, opensearchIndexer);
        vedtakStatusRepository.slettUtkastForBruker(AKTORID);
    }

    @Test
    public void skallSetteInUtkast() {
        utkast14aStatusendringService.behandleKafkaMeldingLogikk(vedtakStatusEndring);
        Optional<Kafka14aStatusendring> endringer = vedtakStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
        assertThat(endringer.get()).isEqualTo(vedtakStatusEndring);
    }

    @Test
    public void skallOppdatereUtkast_sendtutkast() {
        utkast14aStatusendringService.behandleKafkaMeldingLogikk(vedtakStatusEndring);
        LocalDateTime time = DateUtils.now().toLocalDateTime();
        Kafka14aStatusendring kafkaVedtakSendtTilBeslutter = new Kafka14aStatusendring()
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.VEDTAK_SENDT)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(Kafka14aStatusendring.Hovedmal.BEHOLDE_ARBEID)
                .setInnsatsgruppe(Kafka14aStatusendring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        utkast14aStatusendringService.behandleKafkaMeldingLogikk(kafkaVedtakSendtTilBeslutter);

        Optional<Kafka14aStatusendring> endringer = vedtakStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isEmpty()).isTrue();
    }

    @Test
    public void skallSletteGamleVedtak_sendtutkast() {
        vedtakStatusRepository.upsert(new Kafka14aStatusendring()
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.VEDTAK_SENDT)
                .setTimestamp(DateUtils.now().toLocalDateTime())
                .setAktorId(AKTORID)
                .setVedtakId(2)
                .setHovedmal(Kafka14aStatusendring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(Kafka14aStatusendring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS));

        Kafka14aStatusendring kafkaVedtakSendtTilBruker = new Kafka14aStatusendring()
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.VEDTAK_SENDT)
                .setTimestamp(DateUtils.now().toLocalDateTime())
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(Kafka14aStatusendring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(Kafka14aStatusendring.Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        utkast14aStatusendringService.behandleKafkaMeldingLogikk(kafkaVedtakSendtTilBruker);

        Optional<Kafka14aStatusendring> endringer = vedtakStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isEmpty()).isTrue();
    }

    @Test
    public void testJsonDesrializationForVeilederInfo() {
        String inputJsonWithoutVeilederInfo = "{\"vedtakId\":1,\"aktorId\":\"1\",\"vedtakStatusEndring\":\"UTKAST_OPPRETTET\",\"timestamp\":\"2021-02-09T22:24:12.373356+01:00\"}";
        Kafka14aStatusendring kafkaVedtakStatusEndring = fromJson(inputJsonWithoutVeilederInfo, Kafka14aStatusendring.class);

        assertThat(kafkaVedtakStatusEndring.aktorId).isEqualTo("1");
        assertThat(kafkaVedtakStatusEndring.veilederIdent).isNull();
        assertThat(kafkaVedtakStatusEndring.veilederNavn).isNull();

        String inputJsonWithVeilederInfo = "{\"vedtakId\":1,\"aktorId\":\"1\",\"vedtakStatusEndring\":\"UTKAST_OPPRETTET\",\"timestamp\":\"2021-02-09T22:24:12.373356+01:00\", \"veilederIdent\":\"Z1234\", \"veilederNavn\":\"Test123\"}";
        kafkaVedtakStatusEndring = fromJson(inputJsonWithVeilederInfo, Kafka14aStatusendring.class);
        assertThat(kafkaVedtakStatusEndring.veilederNavn).isEqualTo("Test123");
        assertThat(kafkaVedtakStatusEndring.veilederIdent).isEqualTo("Z1234");

    }
}
