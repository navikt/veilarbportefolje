package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaConsumerRegistreringTest extends Thread {

    private MockConsumer<String, ArbeidssokerRegistrertEvent> kafkaConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    private RegistreringRepository registreringRepository = new RegistreringRepository(new JdbcTemplate( setupInMemoryDatabase()));
    private static String AKTORID = "123456789";
    private TopicPartition topicPartition = new TopicPartition("test-topic", 0);

    @Before
    public void setup(){
        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");

        kafkaConsumer.assign(Collections.singletonList(topicPartition));
        kafkaConsumer.updateBeginningOffsets(new HashMap<TopicPartition, Long> (){{put (topicPartition, 0L);}});
        UnleashService unleashService = mock(UnleashService.class);
        when(unleashService.isEnabled(any(String.class))).thenReturn(true);
        new KafkaConsumerRegistrering(new RegistreringService(registreringRepository), kafkaConsumer, unleashService);
    }


    @Test
    public void testConsumer() throws InterruptedException {

        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Situasjon")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now().minusDays(30), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        kafkaConsumer.addRecord(new ConsumerRecord<>("test-topic", 0,
                0L, AKTORID, event2));

        kafkaConsumer.addRecord(new ConsumerRecord<>("test-topic", 0,
                1L, AKTORID, event1));

        Thread.sleep(4000); //VENTER PÅ SERVICEN FÅR BEHANDLET BEGGE MELDINGERNE
        assertThat(registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID))).isEqualTo(event2);

    }
}
