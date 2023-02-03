package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV3;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic.TILTAK_TOPIC;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class LonnstilskuddUtAvArenaTest extends EndToEndTest {

    private final JdbcTemplate jdbcTemplatePostgres;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final TiltakRepositoryV3 tiltakRepositoryV3;
    private final TiltakService tiltakService;
    private final AktorClient aktorClient;
    private final AktivitetService aktivitetService;
    private final OpensearchService opensearchService;


    @Autowired
    public LonnstilskuddUtAvArenaTest(
            JdbcTemplate jdbcTemplatePostgres,
            TiltakRepositoryV2 tiltakRepositoryV2,
            TiltakRepositoryV3 tiltakRepositoryV3,
            TiltakService tiltakService,
            UnleashService unleashService,
            AktorClient aktorClient,
            AktivitetService aktivitetService,
            OpensearchService opensearchService
    ) {
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.tiltakRepositoryV2 = tiltakRepositoryV2;
        this.tiltakRepositoryV3 = tiltakRepositoryV3;
        this.tiltakService = tiltakService;
        this.aktorClient = aktorClient;
        this.aktivitetService = aktivitetService;
        this.opensearchService = opensearchService;
        this.unleashService = unleashService;
    }

    @BeforeEach
    public void resetDb() {
        jdbcTemplatePostgres.update("TRUNCATE aktiviteter");
        jdbcTemplatePostgres.update("TRUNCATE oppfolgingsbruker_arena_v2");
        jdbcTemplatePostgres.update("TRUNCATE bruker_identer");
        jdbcTemplatePostgres.update("TRUNCATE oppfolging_data");
        jdbcTemplatePostgres.update("TRUNCATE brukertiltak");
        jdbcTemplatePostgres.update("TRUNCATE brukertiltak_v2");
        jdbcTemplatePostgres.update("TRUNCATE tiltakkodeverket");
        jdbcTemplatePostgres.update("TRUNCATE lest_arena_hendelse_aktivitet");
    }

    @Test
    public void skal_ikke_returnere_tiltaksdata_naar_status_er_av_type_AktivitetIkkeAktivStatuser() {
        Mockito.when(unleashService.isEnabled(FeatureToggle.STOPP_INDEKSERING_AV_TILTAKSAKTIVITETER)).thenReturn(false);
        NavKontor navKontor = randomNavKontor();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        when(aktorClient.hentAktorId(fnr2)).thenReturn(aktorId2);
        testDataClient.lagreBrukerUnderOppfolging(aktorId1, fnr1, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId2, fnr2, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("VARLONTIL", "Varig lønnstilskudd");

        KafkaAktivitetMelding k1 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2018-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2024-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k2 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId2.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.AVBRUTT)
                .setTiltakskode(til2.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k1);
        aktivitetService.behandleKafkaMeldingLogikk(k2);

        EnhetTiltak tiltak = tiltakService.hentEnhettiltak(EnhetId.of(navKontor.getValue()));
        assertThat(tiltak.getTiltak()).containsExactlyInAnyOrderEntriesOf(Map.of("MIDLONTIL", "Midlertidig lønnstilskudd"));
    }

    @Test
    public void skal_ikke_indeksere_bruker_med_tiltaksaktivitet_data_naar_status_er_av_type_AktivitetIkkeAktivStatuser() {
        Mockito.when(unleashService.isEnabled(FeatureToggle.STOPP_INDEKSERING_AV_TILTAKSAKTIVITETER)).thenReturn(false);
        NavKontor navKontor = randomNavKontor();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        when(aktorClient.hentAktorId(fnr2)).thenReturn(aktorId2);
        testDataClient.lagreBrukerUnderOppfolging(aktorId1, fnr1, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId2, fnr2, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("VARLONTIL", "Varig lønnstilskudd");

        KafkaAktivitetMelding k1 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2018-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2024-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k2 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId2.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.AVBRUTT)
                .setTiltakskode(til2.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k3 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-323456789")
                .setAktorId(aktorId2.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(null)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k1);
        aktivitetService.behandleKafkaMeldingLogikk(k2);
        aktivitetService.behandleKafkaMeldingLogikk(k3);

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall response1 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(response1.getAntall()).isEqualTo(1);

                    BrukereMedAntall response2 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("VARLONTIL")),
                            null,
                            null);

                    assertThat(response2.getAntall()).isEqualTo(0);
                }
        );
    }

    @Test
    public void skal_indeksere_bruker_naar_vi_faar_lonnstilskudd_fra_DAB() {
        Mockito.when(unleashService.isEnabled(FeatureToggle.STOPP_INDEKSERING_AV_TILTAKSAKTIVITETER)).thenReturn(false);
        NavKontor navKontor = randomNavKontor();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        AktorId aktorId3 = randomAktorId();
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        Fnr fnr3 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        when(aktorClient.hentAktorId(fnr2)).thenReturn(aktorId2);
        when(aktorClient.hentAktorId(fnr3)).thenReturn(aktorId3);
        testDataClient.lagreBrukerUnderOppfolging(aktorId1, fnr1, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId2, fnr2, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId3, fnr3, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("VARLONTIL", "Varig lønnstilskudd");

        TiltakInnhold i1a = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype("MENTOR")
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-123456789");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 0, "melding1", new TiltakDTO().setBefore(null).setAfter(i1a)));

        TiltakInnhold i1b = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype(til2.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-567891011");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 0, "melding2", new TiltakDTO().setBefore(null).setAfter(i1b)));


        KafkaAktivitetMelding k1 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.BEHANDLING)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2018-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2024-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(null)
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k2 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(null)
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k3 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k1);
        aktivitetService.behandleKafkaMeldingLogikk(k2);
        aktivitetService.behandleKafkaMeldingLogikk(k3);

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall response1 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(response1.getAntall()).isEqualTo(1);

                    BrukereMedAntall response2 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("VARLONTIL")),
                            null,
                            null);

                    assertThat(response2.getAntall()).isEqualTo(1);
                }
        );
    }

    @Test
    public void skal_lese_melding_og_indeksere_bruker_naar_fraDato_og_tilDato_er_null() {
        Mockito.when(unleashService.isEnabled(FeatureToggle.STOPP_INDEKSERING_AV_TILTAKSAKTIVITETER)).thenReturn(false);
        NavKontor navKontor = randomNavKontor();
        AktorId aktorId1 = randomAktorId();
        Fnr fnr1 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        testDataClient.lagreBrukerUnderOppfolging(aktorId1, fnr1, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");

        KafkaAktivitetMelding k1 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(null)
                .setTilDato(null)
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.PLANLAGT)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k1);

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall response1 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(response1.getAntall()).isEqualTo(1);
                }
        );
    }


    @Test
    public void skal_ikke_indeksere_lonnstilskudd_fra_DAB_naar_featuretoggle_er_enabled() {
        Mockito.when(unleashService.isEnabled(FeatureToggle.STOPP_INDEKSERING_AV_TILTAKSAKTIVITETER)).thenReturn(true);
        NavKontor navKontor = randomNavKontor();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        AktorId aktorId3 = randomAktorId();
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        Fnr fnr3 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        when(aktorClient.hentAktorId(fnr2)).thenReturn(aktorId2);
        when(aktorClient.hentAktorId(fnr3)).thenReturn(aktorId3);
        testDataClient.lagreBrukerUnderOppfolging(aktorId1, fnr1, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId2, fnr2, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId3, fnr3, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("VARLONTIL", "Varig lønnstilskudd");

        TiltakInnhold i1a = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype("MENTOR")
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-123456789");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 0, "melding1", new TiltakDTO().setBefore(null).setAfter(i1a)));

        TiltakInnhold i1b = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype(til2.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-567891011");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 0, "melding2", new TiltakDTO().setBefore(null).setAfter(i1b)));


        KafkaAktivitetMelding k1 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.BEHANDLING)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2018-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2024-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(null)
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k2 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(null)
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k3 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.parse("2021-01-01T00:00:00+02:00"))
                .setFraDato(ZonedDateTime.parse("2017-10-03T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-11-01T00:00:00+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k1);
        aktivitetService.behandleKafkaMeldingLogikk(k2);
        aktivitetService.behandleKafkaMeldingLogikk(k3);

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall response1 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(response1.getAntall()).isEqualTo(0);

                    BrukereMedAntall response2 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("VARLONTIL")),
                            null,
                            null);

                    assertThat(response2.getAntall()).isEqualTo(1);
                }
        );
    }


    @Test
    public void skal_flette_sammen_data_fra_brukertiltak_og_brukertiltakv2_riktig_nar_brukertiltakv2_er_tom() {
        AktorId a1 = randomAktorId();
        AktorId a2 = randomAktorId();
        AktorId a3 = randomAktorId();
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        Fnr fnr3 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(a1);
        when(aktorClient.hentAktorId(fnr2)).thenReturn(a2);
        when(aktorClient.hentAktorId(fnr3)).thenReturn(a3);
        List<AktorId> aktorIdListe = List.of(a1, a2, a3);

        Map.Entry<String, String> til1 = Map.entry("VARLONTIL", "Lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til3 = Map.entry("MENTOR", "Mentor");

        String aktoerIder = aktorIdListe.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result = new HashMap<>(aktorIdListe.size());

        TiltakInnhold i1a = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype(til1.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-123456789");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 0, "melding1", new TiltakDTO().setBefore(null).setAfter(i1a)));

        TiltakInnhold i1b = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-456789101");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 1, "melding2", new TiltakDTO().setBefore(null).setAfter(i1b)));

        TiltakInnhold i2 = new TiltakInnhold()
                .setFnr(fnr2.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype(til2.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2017-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2023-11-01"))
                .setAktivitetid("TA-223456789");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 2, "melding3", new TiltakDTO().setBefore(null).setAfter(i2)));

        TiltakInnhold i3 = new TiltakInnhold()
                .setFnr(fnr3.get())
                .setDeltakerStatus("GJENN")
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");
        tiltakService.behandleKafkaRecord(new ConsumerRecord<>(TILTAK_TOPIC.getTopicName(), 1, 3, "melding4", new TiltakDTO().setBefore(null).setAfter(i3)));

        tiltakRepositoryV3.leggTilTiltak(aktoerIder, result);

        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(a1))
                .extracting(AktivitetEntityDto::getMuligTiltaksNavn)
                .containsExactlyInAnyOrder("MENTOR", "VARLONTIL");
        assertThat(result.get(a2))
                .extracting(AktivitetEntityDto::getMuligTiltaksNavn)
                .containsExactlyInAnyOrder("MIDLONTIL");
        assertThat(result.get(a3))
                .extracting(AktivitetEntityDto::getMuligTiltaksNavn)
                .containsExactlyInAnyOrder("MENTOR");
    }

    @Test
    public void skal_hente_enhetstiltak_riktig() {
        NavKontor navKontor = randomNavKontor();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        AktorId aktorId3 = randomAktorId();
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        Fnr fnr3 = randomFnr();
        when(aktorClient.hentAktorId(fnr1)).thenReturn(aktorId1);
        when(aktorClient.hentAktorId(fnr2)).thenReturn(aktorId2);
        when(aktorClient.hentAktorId(fnr3)).thenReturn(aktorId3);
        testDataClient.lagreBrukerUnderOppfolging(aktorId1, fnr1, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId2, fnr2, navKontor.getValue());
        testDataClient.lagreBrukerUnderOppfolging(aktorId3, fnr3, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("VARLONTIL", "Lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til3 = Map.entry("MENTOR", "Mentor");

        TiltakInnhold i1a = new TiltakInnhold()
                .setTiltakstype(til1.getKey())
                .setTiltaksnavn(til1.getValue())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-123456789");
        tiltakRepositoryV2.upsert(i1a, aktorId1);

        TiltakInnhold i1b = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setTiltaksnavn(til3.getValue())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-456789101");
        tiltakRepositoryV2.upsert(i1b, aktorId1);

        TiltakInnhold i2 = new TiltakInnhold()
                .setTiltakstype(til2.getKey())
                .setTiltaksnavn(til2.getValue())
                .setAktivitetperiodeFra(new ArenaDato("2017-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2023-11-01"))
                .setAktivitetid("TA-223456789");
        tiltakRepositoryV2.upsert(i2, aktorId2);

        TiltakInnhold i3 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setTiltaksnavn(til3.getValue())
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");
        tiltakRepositoryV2.upsert(i3, aktorId3);

        EnhetTiltak et1 = tiltakRepositoryV2.hentTiltakPaEnhet(EnhetId.of(navKontor.getValue()));
        jdbcTemplatePostgres.update("TRUNCATE brukertiltak");

        TiltakInnhold m1 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setTiltaksnavn(til3.getValue())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-456789101");
        tiltakRepositoryV2.upsert(m1, aktorId1);

        TiltakInnhold m2 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setTiltaksnavn(til3.getValue())
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");
        tiltakRepositoryV2.upsert(m2, aktorId3);

        TiltakaktivitetEntity d1a = new TiltakaktivitetEntity()
                .setTiltakskode(til1.getKey())
                .setTiltaksnavn(til1.getValue())
                .setFraDato(new ArenaDato("2018-10-03"))
                .setTilDato(new ArenaDato("2024-11-01"))
                .setAktivitetId("TA-123456789")
                .setStatus("GJENNOMFORES");
        tiltakRepositoryV3.upsert(d1a, aktorId1);

        TiltakaktivitetEntity d2 = new TiltakaktivitetEntity()
                .setTiltakskode(til2.getKey())
                .setTiltaksnavn(til2.getValue())
                .setFraDato(new ArenaDato("2017-10-03"))
                .setTilDato(new ArenaDato("2023-11-01"))
                .setAktivitetId("TA-223456789")
                .setStatus("GJENNOMFORES");
        tiltakRepositoryV3.upsert(d2, aktorId2);

        EnhetTiltak et2 = tiltakRepositoryV3.hentTiltakPaEnhet(EnhetId.of(navKontor.getValue()));

        Map<String, String> forventedeTiltakPaaEnhet = Map.ofEntries(til1, til2, til3);
        assertThat(et1.getTiltak().size()).isEqualTo(et2.getTiltak().size());
        assertThat(et1.getTiltak()).containsExactlyInAnyOrderEntriesOf(forventedeTiltakPaaEnhet);
        assertThat(et2.getTiltak()).containsExactlyInAnyOrderEntriesOf(forventedeTiltakPaaEnhet);
    }

    @Test
    public void skal_flette_sammen_data_fra_brukertiltak_og_brukertiltakv2_riktig() {
        AktorId a1 = randomAktorId();
        AktorId a2 = randomAktorId();
        AktorId a3 = randomAktorId();
        List<AktorId> aktorIdListe = List.of(a1, a2, a3);

        Map.Entry<String, String> til1 = Map.entry("VARLONTIL", "Lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til3 = Map.entry("MENTOR", "Mentor");

        String aktoerIder = aktorIdListe.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result1 = new HashMap<>(aktorIdListe.size());
        HashMap<AktorId, List<AktivitetEntityDto>> result2 = new HashMap<>(aktorIdListe.size());

        TiltakInnhold i1a = new TiltakInnhold()
                .setTiltakstype(til1.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-123456789");
        tiltakRepositoryV2.upsert(i1a, a1);

        TiltakInnhold i1b = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-456789101");
        tiltakRepositoryV2.upsert(i1b, a1);

        TiltakInnhold i2 = new TiltakInnhold()
                .setTiltakstype(til2.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2017-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2023-11-01"))
                .setAktivitetid("TA-223456789");
        tiltakRepositoryV2.upsert(i2, a2);

        TiltakInnhold i3 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");
        tiltakRepositoryV2.upsert(i3, a3);

        tiltakRepositoryV2.leggTilTiltak(aktoerIder, result1);
        jdbcTemplatePostgres.update("TRUNCATE brukertiltak");

        TiltakInnhold m1 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-456789101");
        tiltakRepositoryV2.upsert(m1, a1);

        TiltakInnhold m2 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");
        tiltakRepositoryV2.upsert(m2, a3);

        TiltakaktivitetEntity d1a = new TiltakaktivitetEntity()
                .setTiltakskode(til1.getKey())
                .setFraDato(new ArenaDato("2018-10-03"))
                .setTilDato(new ArenaDato("2024-11-01"))
                .setAktivitetId("TA-123456789")
                .setStatus("GJENNOMFORES");
        tiltakRepositoryV3.upsert(d1a, a1);

        TiltakaktivitetEntity d2 = new TiltakaktivitetEntity()
                .setTiltakskode(til2.getKey())
                .setFraDato(new ArenaDato("2017-10-03"))
                .setTilDato(new ArenaDato("2023-11-01"))
                .setAktivitetId("TA-223456789")
                .setStatus("GJENNOMFORES");
        tiltakRepositoryV3.upsert(d2, a2);

        tiltakRepositoryV3.leggTilTiltak(aktoerIder, result2);

        assertThat(result1).hasSameSizeAs(result2);
        assertThat(result1.get(a1)).hasSameElementsAs(result2.get(a1));
        assertThat(result1.get(a2)).hasSameElementsAs(result2.get(a2));
        assertThat(result1.get(a3)).hasSameElementsAs(result2.get(a3));
    }

    @Test
    public void skal_flette_sammen_data_fra_brukertiltak_og_brukertiltakv2_riktig_nar_de_inneholder_ulik_data_pa_samme_bruker() {

        AktorId a1 = randomAktorId();
        List<AktorId> aktorIdListe = List.of(a1);

        Map.Entry<String, String> til1 = Map.entry("VARLONTIL", "Lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til3 = Map.entry("MENTOR", "Mentor");

        String aktoerIder = aktorIdListe.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result = new HashMap<>(aktorIdListe.size());

        TiltakInnhold m1 = new TiltakInnhold()
                .setTiltakstype(til1.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-456789101");
        tiltakRepositoryV2.upsert(m1, a1);

        TiltakInnhold m2 = new TiltakInnhold()
                .setTiltakstype(til3.getKey())
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");
        tiltakRepositoryV2.upsert(m2, a1);

        TiltakaktivitetEntity d1 = new TiltakaktivitetEntity()
                .setTiltakskode(til1.getKey())
                .setFraDato(new ArenaDato("2018-10-03"))
                .setTilDato(new ArenaDato("2024-11-01"))
                .setAktivitetId("1122")
                .setStatus("GJENNOMFORES");
        tiltakRepositoryV3.upsert(d1, a1);

        TiltakaktivitetEntity d2 = new TiltakaktivitetEntity()
                .setTiltakskode(til2.getKey())
                .setFraDato(new ArenaDato("2017-10-03"))
                .setTilDato(new ArenaDato("2023-11-01"))
                .setAktivitetId("2233")
                .setStatus("GJENNOMFORES");
        tiltakRepositoryV3.upsert(d2, a1);

        tiltakRepositoryV3.leggTilTiltak(aktoerIder, result);

        assertThat(result.get(a1).size()).isEqualTo(3);
    }

    @Test
    public void verifiserAtKafkameldingMedSammeAktivitetIdMenNyVersjonOverskriverGammelVersjon() {
        AktorId aktoer = randomAktorId();
        Fnr fodselsnummer = randomFnr();
        NavKontor navKontor = randomNavKontor();
        Mockito.when(unleashService.isEnabled(FeatureToggle.STOPP_INDEKSERING_AV_TILTAKSAKTIVITETER)).thenReturn(false);

        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("VARLONTIL")
                .setVersion(1L)
                .setAvtalt(true));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(2L)
                .setAvtalt(true));

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukereMIDLONTIL = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    BrukereMedAntall responseBrukereLONNTILS = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("VARLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukereMIDLONTIL.getAntall()).isEqualTo(1);
                    assertThat(responseBrukereLONNTILS.getAntall()).isEqualTo(0);
                    assertThat(responseBrukereMIDLONTIL.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");
                }
        );
    }
}
