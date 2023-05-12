package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV3;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
import no.nav.pto.veilarbportefolje.postgres.AktivitetOpensearchService;
import no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.DateUtils.FAR_IN_THE_FUTURE_DATE;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class TiltakPostgresTest {
    private final JdbcTemplate jdbcTemplatePostgres;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final TiltakRepositoryV3 tiltakRepositoryV3;
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final PdlIdentRepository pdlIdentRepository;

    private final AktorId aktorId = randomAktorId();
    private final Fnr fnr = randomFnr();

    @Autowired
    public TiltakPostgresTest(JdbcTemplate jdbcTemplatePostgres, TiltakRepositoryV2 tiltakRepositoryV2, TiltakRepositoryV3 tiltakRepositoryV3, AktivitetOpensearchService aktivitetOpensearchService, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, PdlIdentRepository pdlIdentRepository) {
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        this.aktivitetOpensearchService = aktivitetOpensearchService;
        this.tiltakRepositoryV2 = tiltakRepositoryV2;
        this.tiltakRepositoryV3  = tiltakRepositoryV3;
        this.pdlIdentRepository = pdlIdentRepository;
    }

    @BeforeAll
    public static void beforeAll() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
    }

    @BeforeEach
    public void reset() {
        jdbcTemplatePostgres.update("TRUNCATE " + PostgresTable.BRUKERTILTAK.TABLE_NAME + " CASCADE");
        jdbcTemplatePostgres.update("TRUNCATE " + PostgresTable.TILTAKKODEVERK.TABLE_NAME + " CASCADE");
        jdbcTemplatePostgres.update("TRUNCATE oppfolgingsbruker_arena_v2 ");
        jdbcTemplatePostgres.update("TRUNCATE bruker_identer ");
    }

    @Test
    public void skal_lagre_tiltak_i_kodeverk_og_på_bruker() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        TiltakInnhold innhold = new TiltakInnhold()
                .setFnr(fnr.get())
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("1989-01-01"))
                .setAktivitetperiodeTil(new ArenaDato("1990-01-01"))
                .setAktivitetid("TA-123456789");
        tiltakRepositoryV2.upsert(innhold, aktorId);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        Optional<String> kodeVerkNavn = tiltakRepositoryV2.hentVerdiITiltakskodeVerk(tiltaksType);

        assertThat(kodeVerkNavn.isPresent()).isTrue();
        assertThat(kodeVerkNavn.get()).isEqualTo(tiltaksNavn);

        //Opensearch mapping
        assertThat(postgresAktivitet.getTiltak().size()).isEqualTo(1);
        assertThat(postgresAktivitet.getTiltak().contains("T123")).isTrue();
        assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.tiltak.name())).isTrue();

        assertThat(postgresAktivitet.getNyesteUtlopteAktivitet().substring(0, 10)).isEqualTo("1990-01-01");
        assertThat(postgresAktivitet.getForrigeAktivitetStart().substring(0, 10)).isEqualTo("1988-12-31");
        assertThat(postgresAktivitet.getAktivitetTiltakUtlopsdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
        assertThat(postgresAktivitet.getNesteAktivitetStart()).isNull();
        assertThat(postgresAktivitet.getAktivitetStart()).isNull();
    }

    @Test
    public void skal_utlede_status_informasjon_basert_paa_neste_kommende_eller_gjeldende_aktivitet() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        ZonedDateTime idagTid = ZonedDateTime.now();
        ZonedDateTime igarTid = ZonedDateTime.now().minusDays(1);

        TiltakInnhold idag = new TiltakInnhold()
                .setFnr(fnr.get())
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setAktivitetperiodeTil(new ArenaDato(idagTid.toString().substring(0, 10)))
                .setAktivitetid("TA-123");
        TiltakInnhold igar = new TiltakInnhold()
                .setFnr(fnr.get())
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setAktivitetperiodeTil(new ArenaDato(igarTid.toString().substring(0, 10)))
                .setAktivitetid("TA-321");

        tiltakRepositoryV2.upsert(idag, aktorId);
        tiltakRepositoryV2.upsert(igar, aktorId);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        assertThat(postgresAktivitet.getTiltak().size()).isEqualTo(1);
        assertThat(postgresAktivitet.getTiltak().contains(tiltaksType)).isTrue();
        assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.tiltak.name())).isTrue();
        assertThat(postgresAktivitet.getAktivitetStart()).isNull();
        assertThat(postgresAktivitet.getNesteAktivitetStart()).isNull();

        assertThat(postgresAktivitet.getNyesteUtlopteAktivitet().substring(0, 10)).isEqualTo(toIsoUTC(igarTid).substring(0, 10));
        assertThat(postgresAktivitet.getAktivitetTiltakUtlopsdato().substring(0, 10)).isEqualTo(toIsoUTC(idagTid).substring(0, 10));
    }

    @Test
    public void skal_lage_slette_tiltak_pa_bruker_men_ikke_kodeverk() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        String id = "TA-123456789";
        TiltakInnhold innhold = new TiltakInnhold()
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setDeltakerStatus("GJENN")
                .setAktivitetid(id);
        tiltakRepositoryV2.upsert(innhold, aktorId);
        tiltakRepositoryV2.delete(id);

        Optional<String> kodeVerkNavn = tiltakRepositoryV2.hentVerdiITiltakskodeVerk(tiltaksType);

        List<AktivitetEntityDto> postgresAktivitet = aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId);

        assertThat(kodeVerkNavn.isPresent()).isTrue();
        assertThat(kodeVerkNavn.get()).isEqualTo(tiltaksNavn);
        assertThat(postgresAktivitet).isNull();
    }

    @Test
    public void skal_lagre_tiltak_pa_enhet() {
        String navKontor = "0007";
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        ));
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null, "" +
                        "Tester", "Testerson", navKontor, null, null, null, null,
                        "1234", true, false, ZonedDateTime.now()));
        String tiltaksType1 = "T123";
        String tiltaksType2 = "T321";
        String tiltaksNavn1 = "test1";
        String tiltaksNavn2 = "test2";

        TiltakInnhold tiltak1 = new TiltakInnhold()
                .setTiltaksnavn(tiltaksNavn1)
                .setTiltakstype(tiltaksType1)
                .setDeltakerStatus("GJENN")
                .setAktivitetid("T-123");

        TiltakInnhold tiltak2 = new TiltakInnhold()
                .setTiltaksnavn(tiltaksNavn2)
                .setTiltakstype(tiltaksType2)
                .setDeltakerStatus("GJENN")
                .setAktivitetid("T-321");

        tiltakRepositoryV2.upsert(tiltak1, aktorId);
        tiltakRepositoryV2.upsert(tiltak2, aktorId);

        EnhetTiltak enhetTiltak = tiltakRepositoryV3.hentTiltakPaEnhet(EnhetId.of(navKontor));
        assertThat(enhetTiltak.getTiltak().size()).isEqualTo(2);
        assertThat(enhetTiltak.getTiltak().get(tiltaksType1)).isEqualTo(tiltaksNavn1);
        assertThat(enhetTiltak.getTiltak().get(tiltaksType2)).isEqualTo(tiltaksNavn2);
    }
}
