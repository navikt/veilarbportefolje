package no.nav.pto.veilarbportefolje.kafka;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepository;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Collections;
import java.util.HashMap;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class KafkaRegistreringRunnableTest {

    private MockConsumer<String, ArbeidssokerRegistrertEvent> kafkaConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    private RegistreringRepository registreringRepository = new RegistreringRepository(new JdbcTemplate( setupInMemoryDatabase()));
    KafkaRegistreringRunnable kafkaRegistreringRunnable;
    private static String AKTORID = "123456789";

    @Before
    public void setup(){
        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");

        kafkaConsumer.assign(Collections.singletonList(new TopicPartition("test-topic", 0)));
        kafkaConsumer.updateBeginningOffsets(new HashMap<TopicPartition, Long> (){{put (new TopicPartition("test-topic", 0), 0L);}});
        kafkaRegistreringRunnable = new KafkaRegistreringRunnable(new RegistreringService(registreringRepository), kafkaConsumer);
    }


    @Test
    public void testConsumer() {

        ArbeidssokerRegistrertEvent event = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setRegistreringOpprettet(null)
                .build();


        kafkaConsumer.addRecord(new ConsumerRecord<>("test-topic", 0,
                0L, AKTORID, event));

        while(kafkaConsumer.position(new TopicPartition("test-topic", 0)) == 0 ) {}
        assertThat(registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID))).isEqualTo(event);

    }
}
