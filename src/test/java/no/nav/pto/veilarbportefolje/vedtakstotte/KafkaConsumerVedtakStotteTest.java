package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.UnleashServiceMock;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.AktoerServiceMock;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

import static no.nav.json.JsonUtils.toJson;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class KafkaConsumerVedtakStotteTest {
    private MockConsumer<String, String> kafkaConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    private VedtakStatusRepository vedtakRepository = new VedtakStatusRepository(new JdbcTemplate(setupInMemoryDatabase()));

    private static String AKTORID = "123456789";
    private TopicPartition topicPartition = new TopicPartition("test-topic", 0);

    @Before
    public void setup(){
        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");

        kafkaConsumer.assign(Collections.singletonList(topicPartition));
        kafkaConsumer.updateBeginningOffsets(new HashMap<TopicPartition, Long>(){{put (topicPartition, 0L);}});
        UnleashService unleashService = new UnleashServiceMock(true);
        ElasticIndexer indexer = mock(ElasticIndexer.class);

        new KafkaConsumerVedtakStotte(new VedtakService(vedtakRepository, indexer, new AktoerServiceMock()), unleashService, kafkaConsumer);
    }


    @Test
    public void testConsumer() throws InterruptedException {

        KafkaVedtakStatusEndring event = new KafkaVedtakStatusEndring()
                .setAktorId(AKTORID)
                .setHovedmal(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(KafkaVedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.UTKAST_OPPRETTET);


        kafkaConsumer.addRecord(new ConsumerRecord<>("test-topic", 0,
                0L, AKTORID, toJson(event)));

        Thread.sleep(2000);
        assertThat(vedtakRepository.hentVedtak(AKTORID).get(0)).isEqualTo(event);

    }
}
