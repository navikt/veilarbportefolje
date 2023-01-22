package no.nav.pto.veilarbportefolje.aktiviteter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.I_AKTIVITET;
import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AktiviteterOpensearchIntegrasjon extends EndToEndTest {
    private final AktivitetService aktivitetService;
    private final TiltakService tiltakService;
    private final OpensearchService opensearchService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final AktorId aktoer = randomAktorId();
    private final Fnr fodselsnummer = Fnr.ofValidFnr("10108000399"); //TESTFAMILIE
    private final JdbcTemplate jdbcTemplatePostgres;
    private final PdlIdentRepository pdlIdentRepository;

    private final TiltakRepositoryV2 tiltakRepositoryV2;

    private final ArenaHendelseRepository arenaHendelseRepository;
    private final AktorClient aktorClient;


    @Autowired
    public AktiviteterOpensearchIntegrasjon(AktivitetService aktivitetService, OpensearchService opensearchService, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, JdbcTemplate jdbcTemplatePostgres, PdlIdentRepository pdlIdentRepository, TiltakRepositoryV2 tiltakRepositoryV2, AktorClient aktorClient, ArenaHendelseRepository arenaHendelseRepository, OpensearchIndexer opensearchIndexer) {
        this.aktivitetService = aktivitetService;
        this.arenaHendelseRepository = arenaHendelseRepository;
        this.tiltakService = new TiltakService(tiltakRepositoryV2, aktorClient, arenaHendelseRepository, opensearchIndexer);
        this.opensearchService = opensearchService;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.pdlIdentRepository = pdlIdentRepository;
        this.tiltakRepositoryV2 = tiltakRepositoryV2;
        this.aktorClient = aktorClient;
    }

    @BeforeEach
    public void resetDb() {
        jdbcTemplatePostgres.update("TRUNCATE aktiviteter");
        jdbcTemplatePostgres.update("TRUNCATE oppfolgingsbruker_arena_v2");
        jdbcTemplatePostgres.update("TRUNCATE bruker_identer");
        jdbcTemplatePostgres.update("TRUNCATE oppfolging_data");
        jdbcTemplatePostgres.update("TRUNCATE brukertiltak");
        jdbcTemplatePostgres.update("TRUNCATE lest_arena_hendelse_aktivitet");
    }

    @Test
    public void lasteroppeikkelagreteaktiviteteter() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of(I_AKTIVITET)),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                }
        );
    }

    @Test
    public void lasterOppTiltaksaktivitet() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.parse("2023-05-25T00:00:00+02:00"))
                .setTilDato(ZonedDateTime.parse("2023-10-03T23:59:59+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(1L)
                .setAvtalt(true));

        List<BrukerTiltak> fraTiltakDB = hentFelterBrukerTiltakPaAktorId(aktoer);
        assertThat(fraTiltakDB.size()).isEqualTo(1);
        assertThat(fraTiltakDB.get(0).version).isEqualTo(1L);
        assertThat(fraTiltakDB.get(0).fradato).isEqualTo(Timestamp.valueOf("2023-05-25 00:00:00.0"));
        assertThat(fraTiltakDB.get(0).tildato).isEqualTo(Timestamp.valueOf("2023-10-03 23:59:59.0"));
        assertThat(fraTiltakDB.get(0).tiltakskode).isEqualTo("MIDLONTIL");

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");
                }
        );
    }

    @Test
    public void verifiserAtKafkameldingMedSammeAktivitetIdMenNyVersjonOverskriverGammelVersjon() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("LONNTILS")
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

        assertThat(hentFelterBrukerTiltakPaAktivitetId("2").size()).isEqualTo(1);
        assertThat(hentFelterBrukerTiltakPaAktivitetId("2").get(0).getVersion()).isEqualTo(2L);

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
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("LONNTILS")),
                            null,
                            null);

                    assertThat(responseBrukereMIDLONTIL.getAntall()).isEqualTo(1);
                    assertThat(responseBrukereLONNTILS.getAntall()).isEqualTo(0);
                    assertThat(responseBrukereMIDLONTIL.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");

                }
        );
    }

    @Test
    public void skal_laste_inn_samme_data_fra_ny_datakilde_korrekt() {
        NavKontor navKontor = randomNavKontor();
        AktorId a1 = randomAktorId();
        Fnr f1 = randomFnr();
        Mockito.when(aktorClient.hentAktorId(f1)).thenReturn(a1);
        testDataClient.lagreBrukerUnderOppfolging(a1, f1, navKontor.getValue());
        AktorId a2 = randomAktorId();
        Fnr f2 = randomFnr();
        Mockito.when(aktorClient.hentAktorId(f2)).thenReturn(a2);
        testDataClient.lagreBrukerUnderOppfolging(a2, f2, navKontor.getValue());
        AktorId a3 = randomAktorId();
        Fnr f3 = randomFnr();
        Mockito.when(aktorClient.hentAktorId(f3)).thenReturn(a3);
        testDataClient.lagreBrukerUnderOppfolging(a3, f3, navKontor.getValue());

        Map.Entry<String, String> til1 = Map.entry("LONNTILS", "Lønnstilskudd");
        Map.Entry<String, String> til2 = Map.entry("MIDLONTIL", "Midlertidig lønnstilskudd");
        Map.Entry<String, String> til3 = Map.entry("MENTOR", "Mentor");

        sendKafkaMeldingerFraArena(f1, f2, f3, til1, til2, til3);

        sendKafkaMeldingerFraDAB(a1, a2, a3, til1, til2, til3);

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBruker1 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setNavnEllerFnrQuery(f1.get()),
                            null,
                            null);
                    // forventer at bruker1 har tt2
                    assertThat(responseBruker1.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("LONNTILS");

                    BrukereMedAntall responseBruker2 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setNavnEllerFnrQuery(f2.get()),
                            null,
                            null);

                    // forventer at bruker2 har ingen tiltaksaktivitet
                    assertThat(responseBruker2.getBrukere().get(0).getBrukertiltak().size()).isEqualTo(0);

                    BrukereMedAntall responseBruker3 = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setNavnEllerFnrQuery(f3.get()),
                            null,
                            null);

                    // forventer at bruker3 har tt3
                    assertThat(responseBruker3.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MENTOR");
                }
        );
    }

    private void sendKafkaMeldingerFraArena(Fnr fnr1, Fnr fnr2, Fnr fnr3, Map.Entry<String, String> til1, Map.Entry<String, String> til2, Map.Entry<String, String> til3) {
        // 1. Lag 3 Kafka-meldinger m1, m2 og m3 for 3 nye unike tiltaksaktiviteter, med henholdsvis tiltakId a1, a2, a3 for bruker b1, b2 og b3
        // 2. Lag Kafka-melding som oppdaterer a1 med ny tiltakstype (tt2) og navn
        // 3. Lag Kafka-melding som sletter a2
        // 4. Send m1 på nytt
        // 5. Lag Kafka-melding som sletter a3
        // 6. Lag Kafka-melding m4 med ny tiltakstype (tt3) a4 for bruker b3


        TiltakInnhold i1 = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setTiltaksnavn(til1.getValue())
                .setTiltakstype(til1.getKey())
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-123456789");

        TiltakInnhold i2 = new TiltakInnhold()
                .setFnr(fnr2.get())
                .setTiltaksnavn(til2.getValue())
                .setTiltakstype(til2.getKey())
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2017-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2023-11-01"))
                .setAktivitetid("TA-223456789");

        TiltakInnhold i3 = new TiltakInnhold()
                .setFnr(fnr3.get())
                .setTiltaksnavn(til3.getValue())
                .setTiltakstype(til3.getKey())
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2016-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2022-11-01"))
                .setAktivitetid("TA-323456789");

        TiltakDTO m1 = new TiltakDTO().setBefore(null).setAfter(i1);
        TiltakDTO m2 = new TiltakDTO().setBefore(null).setAfter(i2);
        TiltakDTO m3 = new TiltakDTO().setBefore(null).setAfter(i3);

        tiltakService.behandleKafkaMelding(m1);
        tiltakService.behandleKafkaMelding(m2);
        tiltakService.behandleKafkaMelding(m3);

        TiltakInnhold i1_ny = new TiltakInnhold()
                .setFnr(fnr1.get())
                .setTiltaksnavn(til2.getValue())
                .setTiltakstype(til2.getKey())
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2018-11-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-10-01"))
                .setAktivitetid("TA-123456789");

        TiltakDTO m4 = new TiltakDTO().setBefore(i1).setAfter(i1_ny);
        tiltakService.behandleKafkaMelding(m4);

        TiltakDTO m5 = new TiltakDTO().setBefore(i2).setAfter(null);
        tiltakService.behandleKafkaMelding(m5);

        tiltakService.behandleKafkaMelding(m1);

        TiltakDTO m6 = new TiltakDTO().setBefore(i3).setAfter(null);
        tiltakService.behandleKafkaMelding(m6);

        TiltakInnhold i4 = new TiltakInnhold()
                .setFnr(fnr3.get())
                .setTiltaksnavn(til3.getValue())
                .setTiltakstype(til3.getKey())
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2020-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("TA-423456789");
        TiltakDTO m7 = new TiltakDTO().setBefore(null).setAfter(i4);
        tiltakService.behandleKafkaMelding(m7);

    }

    private void sendKafkaMeldingerFraDAB(AktorId aktorId1, AktorId aktorId2, AktorId aktorId3, Map.Entry<String, String> til1, Map.Entry<String, String> til2, Map.Entry<String, String> til3) {
        // 1. Lag 3 Kafka-meldinger m1, m2 og m3 for 3 nye unike tiltaksaktiviteter, med henholdsvis tiltakId a1, a2, a3 for bruker b1, b2 og b3
        // 2. Lag Kafka-melding som oppdaterer a1 med ny tiltakstype (tt2) og navn
        // 3. Lag Kafka-melding som sletter a2
        // 4. Send m1 på nytt
        // 5. Lag Kafka-melding som sletter a3
        // 6. Lag Kafka-melding m4 med ny tiltakstype (tt3) a4 for bruker b3
        KafkaAktivitetMelding k1 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2021-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2018-10-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2024-11-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til1.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k2 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-223456789")
                .setAktorId(aktorId2.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2021-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2017-10-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2023-11-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til2.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        KafkaAktivitetMelding k3 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-323456789")
                .setAktorId(aktorId3.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2021-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2016-10-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2022-11-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til3.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k1);
        aktivitetService.behandleKafkaMeldingLogikk(k2);
        aktivitetService.behandleKafkaMeldingLogikk(k3);

        KafkaAktivitetMelding k4 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-123456789")
                .setAktorId(aktorId1.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2021-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2018-11-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2024-10-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til2.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k4);

        aktivitetService.behandleKafkaMeldingLogikk(k2.setHistorisk(true));

        aktivitetService.behandleKafkaMeldingLogikk(k1);

        aktivitetService.behandleKafkaMeldingLogikk(k3.setHistorisk(true));

        KafkaAktivitetMelding k5 = new KafkaAktivitetMelding()
                .setAktivitetId("TA-323456789")
                .setAktorId(aktorId3.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2022-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2022-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2027-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(til3.getKey())
                .setVersion(1L)
                .setAvtalt(true)
                .setHistorisk(false);

        aktivitetService.behandleKafkaMeldingLogikk(k5);
    }


    @Test
    public void brukertiltakErOppdatert() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(1L)
                .setAvtalt(true));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("3")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("LONNTILS")
                .setVersion(1L)
                .setAvtalt(true));

        List<BrukerTiltak> brukertiltak= hentFelterBrukerTiltakPaAktorId(aktoer);
        assertThat(brukertiltak.size()).isEqualTo(2);

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukere.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");
                }
        );
    }

    @Test
    public void lasteroppaktivitetStillingFraNAV() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.STILLING_FRA_NAV)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(null)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false)
                .setStillingFraNavData(
                        new KafkaAktivitetMelding.StillingFraNAV()
                                .setCvKanDelesStatus(KafkaAktivitetMelding.CvKanDelesStatus.JA)
                                .setSvarfrist("2044-02-03T00:00:00+02:00"))
        );
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setNavnEllerFnrQuery(fodselsnummer.toString()).setFerdigfilterListe(new ArrayList<>()),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().get(0).getNesteCvKanDelesStatus()).isEqualTo("JA");
                    assertThat(responseBrukere.getBrukere().get(0).getNesteSvarfristCvStillingFraNav()).isEqualTo(LocalDate.parse("2044-02-03"));
                }
        );
    }

    @Test
    public void filtrerBrukerePaStillingFraNAV() {
        NavKontor navKontor = randomNavKontor();
        AktorId aktoer1 = randomAktorId();
        AktorId aktoer2 = randomAktorId();
        VeilederId veileder = randomVeilederId();
        testDataClient.lagreBrukerUnderOppfolging(aktoer1, navKontor, veileder, ZonedDateTime.now());
        testDataClient.lagreBrukerUnderOppfolging(aktoer2, navKontor, veileder, ZonedDateTime.now());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer1.toString())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.STILLING_FRA_NAV)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(null)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false)
                .setStillingFraNavData(
                        new KafkaAktivitetMelding.StillingFraNAV()
                                .setCvKanDelesStatus(KafkaAktivitetMelding.CvKanDelesStatus.JA)
                                .setSvarfrist("2044-02-03T00:00:00+02:00"))
        );
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("4")
                .setAktorId(aktoer2.toString())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(null)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(true)
        );
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
            BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                    navKontor.getValue(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg().setFerdigfilterListe(List.of(I_AKTIVITET)),
                    null,
                    null);

            assertThat(responseBrukere.getAntall()).isEqualTo(2);
        });


        BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                navKontor.getValue(),
                empty(),
                "asc",
                "ikke_satt",
                new Filtervalg().setStillingFraNavFilter(List.of(StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA)).setFerdigfilterListe(new ArrayList<>()),
                null,
                null);

        System.out.println(JsonUtils.toJson(new Filtervalg().setStillingFraNavFilter(List.of(StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA)).setFerdigfilterListe(new ArrayList<>())));
        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertThat(responseBrukere.getBrukere().get(0).getNesteCvKanDelesStatus()).isEqualTo("JA");
        assertThat(responseBrukere.getBrukere().get(0).getNesteSvarfristCvStillingFraNav()).isEqualTo(LocalDate.parse("2044-02-03"));


    }

    @Test
    public void hentMoteplan() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();
        VeilederId annenVeileder = randomVeilederId();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, navKontor, veileder, ZonedDateTime.now());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now().plusDays(2))
                .setTilDato(ZonedDateTime.now().plusDays(2))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(true));
        // Møte satt tilbake i tid
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("3")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now().minusDays(2))
                .setTilDato(ZonedDateTime.now().minusDays(2))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(true));
        List<Moteplan> moteplaner = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));
        List<Moteplan> ingenMotePlaner = aktivitetService.hentMoteplan(annenVeileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));

        assertThat(moteplaner.size()).isEqualTo(2);
        assertThat(ingenMotePlaner.size()).isEqualTo(0);
    }

    @Test
    public void hentMoteplan_sperretAnsatt() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();

        testDataClient.lagreBrukerUnderOppfolging(aktoer, navKontor, veileder, ZonedDateTime.now());
        settSperretAnsatt(aktoer, navKontor);

        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        List<Moteplan> medTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, true));
        List<Moteplan> utenTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));

        assertThat(medTilgang.stream().noneMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
        assertThat(utenTilgang.stream().allMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
    }


    @Test
    public void hentMoteplan_diskresjonsKode() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();

        AktorId aktoerKode6 = randomAktorId();
        AktorId aktoerKode7 = randomAktorId();

        testDataClient.lagreBrukerUnderOppfolging(aktoerKode6, navKontor, veileder, ZonedDateTime.now());
        testDataClient.lagreBrukerUnderOppfolging(aktoerKode7, navKontor, veileder, ZonedDateTime.now());
        settDiskresjonskode(aktoerKode6, navKontor, "6");
        settDiskresjonskode(aktoerKode7, navKontor, "7");

        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoerKode6.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoerKode7.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        List<Moteplan> medTilgang_alt = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(true, true, false));
        List<Moteplan> medTilgang_6 = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(true, false, false));
        List<Moteplan> medTilgang_7 = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, true, false));
        List<Moteplan> utenTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));

        assertThat(medTilgang_alt.stream().noneMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
        assertThat(medTilgang_6.stream().filter(moteplan -> moteplan.deltaker().equals(skjermetDeltaker)).toList().size()).isEqualTo(1);
        assertThat(medTilgang_7.stream().filter(moteplan -> moteplan.deltaker().equals(skjermetDeltaker)).toList().size()).isEqualTo(1);
        assertThat(utenTilgang.stream().allMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
    }

    private void settSperretAnsatt(AktorId aktorId, NavKontor navKontor) {
        Fnr fnr = pdlIdentRepository.hentFnr(aktorId);
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null,
                        "test", "testson", navKontor.getValue(), null, null,
                        null, null, null, true,
                        false, ZonedDateTime.now()));
    }

    private void settDiskresjonskode(AktorId aktorId, NavKontor navKontor, String kode) {
        Fnr fnr = pdlIdentRepository.hentFnr(aktorId);
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null,
                        "test", "testson", navKontor.getValue(), null, null,
                        null, null, kode, false, false,
                        ZonedDateTime.now()));
    }



    public List<BrukerTiltak> hentFelterBrukerTiltakPaAktorId(AktorId aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? ", "brukertiltak", "aktoerid");
        return jdbcTemplatePostgres.queryForList(sql, aktorId.get())
                .stream().map(this::brukertiltakMapper)
                .toList();
    }

    public List<BrukerTiltak> hentFelterBrukerTiltakPaAktivitetId(String aktivitetid) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? ", "brukertiltak", "aktivitetid");
        return jdbcTemplatePostgres.queryForList(sql, aktivitetid)
                .stream().map(this::brukertiltakMapper)
                .toList();
    }

    public Optional<Timestamp> hentfradatoBrukerTiltak(AktorId aktorId, String aktivitetid) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ? ", "fradato", "brukertiltak", "aktivitetid");
        return Optional.ofNullable(
                queryForObjectOrNull(() -> jdbcTemplatePostgres.queryForObject(sql, (rs, row) -> rs.getTimestamp("fradato"), aktivitetid))
        );
    }

    @SneakyThrows
    public BrukerTiltak brukertiltakMapper (Map<String, Object> row) {
        return new
                BrukerTiltak()
                .setAktivitetid((String) row.get("aktivitetid"))
                .setPersonid(Integer.valueOf((String) row.get("personid")))
                .setAktorid((String) row.get("aktoerid"))
                .setTiltakskode((String) row.get("tiltakskode"))
                .setFradato((Timestamp) row.get("fradato"))
                .setTildato((Timestamp) row.get("tildato"))
                .setVersion((Long) row.get("version"));
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private class BrukerTiltak{
        String aktivitetid;
        Integer personid;
        String aktorid;
        String tiltakskode;
        Timestamp fradato;
        Timestamp tildato;
        Long version;
    }
}
