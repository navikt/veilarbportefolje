package no.nav.pto.veilarbportefolje.kafka;

import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogFeedRepository;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.mockito.Mockito.mock;

public class KafkaDialogServiceRunnableTest {

    MockConsumer<String, String> kafkaConsumer;
    KafkaDialogServiceRunnable kafkaDialogServiceRunnable;

    @Before
    public void setup(){
        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
        DialogFeedRepository dialogFeedRepository = new DialogFeedRepository(new JdbcTemplate( setupInMemoryDatabase()));

        kafkaConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        kafkaDialogServiceRunnable = new KafkaDialogServiceRunnable(dialogFeedRepository,  mock(UnleashService.class), kafkaConsumer, mock(ElasticIndexer.class));
    }

    @Test
    public void testConsumer() throws IOException {
        kafkaConsumer.assign(Arrays.asList(new TopicPartition("my_topic", 0)));
        HashMap<TopicPartition, Long> beginningOffsets = new HashMap<>();
        beginningOffsets.put(new TopicPartition("my_topic", 0), 0L);
        kafkaConsumer.updateBeginningOffsets(beginningOffsets);
    }
}
