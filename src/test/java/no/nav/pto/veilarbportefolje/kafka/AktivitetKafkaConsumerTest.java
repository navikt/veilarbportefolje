package no.nav.pto.veilarbportefolje.kafka;

import io.vavr.control.Try;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.PersonIdService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.TestUtil.createUnleashMock;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO.AKTIVITETER;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AktivitetKafkaConsumerTest extends IntegrationTest {

    private static AktivitetService aktivitetService;
    private static AktivitetDAO aktivitetDAO;
    private static JdbcTemplate jdbcTemplate;
    private static String indexName;
    private static PersonIdService personIdService;
    private static BrukerRepository brukerRepository;
    static String aktoerId = "123456789";
    static String aktivitetId = "144500";

    static String tilDato = (LocalDate.now().plusMonths(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toString();


    @BeforeClass
    public static void beforeClass() {

        DataSource dataSource = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        indexName = generateId();

        aktivitetDAO = new AktivitetDAO(jdbcTemplate, namedParameterJdbcTemplate);
        brukerRepository = mock(BrukerRepository.class);

        personIdService = mock(PersonIdService.class);

        PersistentOppdatering persistentOppdatering = new PersistentOppdatering(new ElasticIndexer(aktivitetDAO, brukerRepository, ELASTIC_CLIENT, mock(UnleashService.class), mock(MetricsClient.class), indexName), brukerRepository, aktivitetDAO);

        aktivitetService = new AktivitetService(aktivitetDAO, persistentOppdatering, personIdService);


        new KafkaConsumerRunnable<>(
                aktivitetService,
                createUnleashMock(),
                getKafkaConsumerProperties(),
                KAFKA_AKTIVITER_CONSUMER_TOPIC,
                ""
        );
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE " + AKTIVITETER);
        jdbcTemplate.execute("TRUNCATE TABLE BRUKERSTATUS_AKTIVITETER");
        deleteIndex(indexName);
    }

    @Test
    public void skal_inserte_kafka_melding_i_db () throws InterruptedException {
        Fnr fnr1 = Fnr.of("11111111111");
        when(personIdService.hentPersonidFraAktoerid(AktoerId.of(aktoerId))).thenReturn(Try.success(PersonId.of("1234")));
        when(brukerRepository.hentBrukere(any(List.class))).thenReturn(Collections.singletonList(new OppfolgingsBruker().setPerson_id("1234").setFnr(fnr1.getFnr())));


        createAktivitetDocument(fnr1);
        populateKafkaAktvitetTopic();

        pollUntilHarOppdatertIElastic(()-> untilOppdatertAktivitetIJobbUtlopsdato(fnr1));
        assertThat(getAktivitetIJobbUtlopsdato(fetchDocument(indexName, fnr1))).isEqualTo(toIsoUTC(timestampFromISO8601(tilDato)));


    }


    private Boolean untilOppdatertAktivitetIJobbUtlopsdato(Fnr fnr) {
        return Optional.of(fetchDocument(indexName, fnr))
                .map(AktivitetKafkaConsumerTest::getAktivitetIJobbUtlopsdato)
                .map(utlopsDato -> utlopsDato.equals(getFarInTheFutureDate()))
                .get();
    }

    private static String getAktivitetIJobbUtlopsdato(GetResponse get1) {
        return (String) get1.getSourceAsMap().get("aktivitet_ijobb_utlopsdato");
    }


    private void createAktivitetDocument(Fnr fnr) {
        String document = new JSONObject()
                .put("aktivitet_mote_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_stilling_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_egen_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_behandling_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_ijobb_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_sokeavtale_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_tiltak_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_utdanningaktivitet_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_gruppeaktivitet_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("fnr", fnr.toString())
                .toString();

        createDocument(indexName, fnr, document);
    }


    private static void populateKafkaAktvitetTopic() {
        String aktivitetIJobbKafkaMelding = new JSONObject()
                .put("aktivitetId", aktivitetId)
                .put("aktorId", aktoerId)
                .put("fraDato", "2020-08-31T10:03:20+02:00")
                .put("tilDato", tilDato)
                .put("endretDato","2020-07-29T15:43:41.049+02:00")
                .put("aktivitetType", "IJOBB")
                .put("aktivitetStatus", "GJENNOMFORES")
                .put("avtalt", true)
                .put("historisk", false)
                .toString();

        populateKafkaTopic(KAFKA_AKTIVITER_CONSUMER_TOPIC.topic, aktoerId, aktivitetIJobbKafkaMelding);
    }
}
