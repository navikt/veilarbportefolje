package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolginsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.postgres.opensearch.AktivitetOpensearchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class TiltakPostgresTest {
    private final JdbcTemplate db;
    private final OppfolginsbrukerRepositoryV2 oppfolginsbrukerRepositoryV2;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2;
    private final AktivitetOpensearchMapper aktivitetOpensearchMapper;

    private final AktorId aktorId = AktorId.of("123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final PersonId personId = PersonId.of("123");

    @Autowired
    public TiltakPostgresTest(@Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplatePostgres, TiltakRepositoryV2 tiltakRepositoryV2, AktivitetOpensearchMapper aktivitetOpensearchMapper) {
        this.db = jdbcTemplatePostgres;
        this.oppfolginsbrukerRepositoryV2 = new OppfolginsbrukerRepositoryV2(db);
        this.aktivitetStatusRepositoryV2 = new AktivitetStatusRepositoryV2(db);
        this.aktivitetOpensearchMapper = aktivitetOpensearchMapper;
        this.tiltakRepositoryV2 = tiltakRepositoryV2;
    }

    @BeforeEach
    public void reset() {
        db.update("TRUNCATE " + PostgresTable.BRUKERTILTAK.TABLE_NAME + " CASCADE");
        db.update("TRUNCATE " + PostgresTable.TILTAKKODEVERK.TABLE_NAME + " CASCADE");
        db.update("TRUNCATE " + PostgresTable.AKTIVITETTYPE_STATUS.TABLE_NAME + " CASCADE");
        db.update("TRUNCATE " + PostgresTable.AKTIVITET_STATUS.TABLE_NAME + " CASCADE");
        db.update("TRUNCATE " + PostgresTable.OPPFOLGINGSBRUKER_ARENA.TABLE_NAME + " CASCADE");
    }

    @Test
    public void skal_lagre_tiltak_i_kodeverk_og_på_bruker() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        TiltakInnhold innhold = new TiltakInnhold()
                .setFnr(fnr.get())
                .setPersonId(personId.toInteger())
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeTil(new ArenaDato("1990-01-01"))
                .setAktivitetid("TA-123456789");
        tiltakRepositoryV2.upsert(innhold, aktorId);

        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(aktorId);

        List<String> tiltak = tiltakRepositoryV2.hentBrukertiltak(aktorId);
        Optional<AktivitetStatus> aktivitetStatus = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), AktivitetTyper.tiltak.name());
        Optional<Timestamp> utloptAktivitet = aktivitetStatusRepositoryV2.hentAktivitetStatusUtlopt(aktorId.get());

        Optional<String> kodeVerkNavn = tiltakRepositoryV2.hentVerdiITiltakskodeVerk(tiltaksType);

        assertThat(tiltak.size()).isEqualTo(1);
        assertThat(tiltak.get(0)).isEqualTo(tiltaksType);

        assertThat(aktivitetStatus.isPresent()).isTrue();
        assertThat(aktivitetStatus.get().isAktiv()).isTrue();
        assertThat(aktivitetStatus.get().getNesteUtlop()).isNull();

        assertThat(kodeVerkNavn.isPresent()).isTrue();
        assertThat(kodeVerkNavn.get()).isEqualTo(tiltaksNavn);

        assertThat(utloptAktivitet.isPresent()).isTrue();
        assertThat(utloptAktivitet.get().toLocalDateTime().getYear()).isEqualTo(1990);
    }

    @Test
    public void skal_utlede_status_informasjon_basert_paa_neste_kommende_eller_gjeldende_aktivitet() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        ZonedDateTime idagTid = ZonedDateTime.now();
        ZonedDateTime igarTid = ZonedDateTime.now().minusDays(1);

        TiltakInnhold idag = new TiltakInnhold()
                .setFnr(fnr.get())
                .setPersonId(personId.toInteger())
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setAktivitetperiodeTil(new ArenaDato(idagTid.toString().substring(0, 10)))
                .setAktivitetid("TA-123");
        TiltakInnhold igar = new TiltakInnhold()
                .setFnr(fnr.get())
                .setPersonId(personId.toInteger())
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setAktivitetperiodeTil(new ArenaDato(igarTid.toString().substring(0, 10)))
                .setAktivitetid("TA-321");

        tiltakRepositoryV2.upsert(idag, aktorId);
        tiltakRepositoryV2.upsert(igar, aktorId);

        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(aktorId);

        Optional<AktivitetStatus> aktivitetStatus = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), AktivitetTyper.tiltak.name());
        Optional<Timestamp> utloptAktivitet = aktivitetStatusRepositoryV2.hentAktivitetStatusUtlopt(aktorId.get());

        assertThat(aktivitetStatus.isPresent()).isTrue();
        assertThat(utloptAktivitet.isPresent()).isTrue();

        assertThat(aktivitetStatus.get().getNesteUtlop().toLocalDateTime().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(utloptAktivitet.get().toLocalDateTime().toLocalDate()).isEqualTo(LocalDate.now().minusDays(1));
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

        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(aktorId);

        List<String> tiltak = tiltakRepositoryV2.hentBrukertiltak(aktorId);
        Optional<AktivitetStatus> aktivitetStatus = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), AktivitetTyper.tiltak.name());
        Optional<Timestamp> utloptAktivitet = aktivitetStatusRepositoryV2.hentAktivitetStatusUtlopt(aktorId.get());

        Optional<String> kodeVerkNavn = tiltakRepositoryV2.hentVerdiITiltakskodeVerk(tiltaksType);

        assertThat(tiltak.size()).isEqualTo(0);
        assertThat(kodeVerkNavn.isPresent()).isTrue();
        assertThat(kodeVerkNavn.get()).isEqualTo(tiltaksNavn);

        assertThat(aktivitetStatus.isPresent()).isTrue();
        assertThat(aktivitetStatus.get().isAktiv()).isFalse();
        assertThat(utloptAktivitet.isPresent()).isFalse();
    }

    @Test
    public void skal_lagre_tiltak_pa_enhet() {
        String navKontor = "0007";
        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktorId.get(), null, null, null, "" +
                        "Tester", "Testerson", navKontor, null, null, null, null,
                        "1234", true, true, false, null, ZonedDateTime.now()));
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

        EnhetTiltak enhetTiltak = tiltakRepositoryV2.hentTiltakPaEnhet(EnhetId.of(navKontor));
        assertThat(enhetTiltak.getTiltak().size()).isEqualTo(2);
        assertThat(enhetTiltak.getTiltak().get(tiltaksType1)).isEqualTo(tiltaksNavn1);
        assertThat(enhetTiltak.getTiltak().get(tiltaksType2)).isEqualTo(tiltaksNavn2);
    }
}
