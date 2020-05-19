package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.UnleashServiceMock;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerRunnable;
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
import static org.mockito.Mockito.mock;

public class RegistreringServiceTest {

    private RegistreringRepository registreringRepository = new RegistreringRepository(new JdbcTemplate(setupInMemoryDatabase()));
    private static String AKTORID = "123456789";
    private ElasticIndexer elasticMock = mock(ElasticIndexer.class);
    private RegistreringService registreringService;

    @Before
    public void setup(){
        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
       this.registreringService = new RegistreringService(registreringRepository, elasticMock);
    }


    @Test
    public void testConsumer() {

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

        registreringService.behandleKafkaMelding(event1);
        registreringService.behandleKafkaMelding(event2);
        assertThat(registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID))).isEqualTo(event2);

    }

    @Test
    public void skaHanteraAttRegisteringOpprettetErNullIDatabasen() {

        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Situasjon")
                .setRegistreringOpprettet(null)
                .build();

        registreringRepository.insertBrukerRegistrering(event1);

        ArbeidssokerRegistrertEvent kafkaMelding = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();


        registreringService.behandleKafkaMelding(kafkaMelding);
        assertThat(registreringRepository.hentBrukerRegistrering(AktoerId.of(AKTORID))).isEqualTo(kafkaMelding);
    }
}
