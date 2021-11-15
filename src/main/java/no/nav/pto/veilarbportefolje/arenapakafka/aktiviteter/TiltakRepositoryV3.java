package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.BrukertiltakV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.Tiltakkodeverk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.AKTIVITETID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.FRADATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.PERSONID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.TILDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.TILTAKSKODE;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV3 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;
    private final AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2;

    public void upsert(TiltakInnhold innhold, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeFra(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeTil(), true);

        log.info("Lagrer tiltak: {}", innhold.getAktivitetid());

        if (skalOppdatereTiltakskodeVerk(innhold.getTiltakstype(), innhold.getTiltaksnavn())) {
            upsertTiltakKodeVerk(innhold);
        }
        db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTIVITETID + ", " + PERSONID + ", " + AKTOERID + ", " + TILTAKSKODE + ", " + FRADATO + ", " + TILDATO + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTIVITETID + ") " +
                        "DO UPDATE SET (" + PERSONID + ", " + AKTOERID + ", " + TILTAKSKODE + ", " + FRADATO + ", " + TILDATO + ") = (?, ?, ?, ?, ?)",
                innhold.getAktivitetid(),
                String.valueOf(innhold.getPersonId()), aktorId.get(), innhold.getTiltakstype(), fraDato, tilDato,
                String.valueOf(innhold.getPersonId()), aktorId.get(), innhold.getTiltakstype(), fraDato, tilDato
        );
    }

    private void upsertTiltakKodeVerk(TiltakInnhold innhold) {
        db.update("INSERT INTO " + PostgresTable.TILTAKKODEVERK.TABLE_NAME +
                        " (" + PostgresTable.TILTAKKODEVERK.KODE + ", " + PostgresTable.TILTAKKODEVERK.VERDI + ") " +
                        "VALUES (?, ?) " +
                        "ON CONFLICT (" + PostgresTable.TILTAKKODEVERK.KODE + ") " +
                        "DO UPDATE SET " + PostgresTable.TILTAKKODEVERK.VERDI + " = ?",
                innhold.getTiltakstype(), innhold.getTiltaksnavn(),
                innhold.getTiltaksnavn()
        );
    }

    public void delete(String tiltakId) {
        log.info("Sletter tiltak: {}", tiltakId);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTIVITETID), tiltakId);
    }

    public EnhetTiltak hentTiltakPaEnhet(EnhetId enhetId) {
        final String hentTiltakPaEnhetSql = "SELECT * FROM " + PostgresTable.TILTAKKODEVERK.TABLE_NAME + " WHERE " +
                PostgresTable.TILTAKKODEVERK.KODE + " IN (SELECT DISTINCT " + TILTAKSKODE + " FROM " + TABLE_NAME +
                " BT INNER JOIN " + PostgresTable.OPPFOLGINGSBRUKER_ARENA.TABLE_NAME + " OP ON BT." + AKTOERID + " = OP." + PostgresTable.OPPFOLGINGSBRUKER_ARENA.AKTOERID +
                " WHERE OP." + PostgresTable.OPPFOLGINGSBRUKER_ARENA.NAV_KONTOR + "=?)";

        return new EnhetTiltak().setTiltak(
                db.queryForList(hentTiltakPaEnhetSql, enhetId.get())
                        .stream().map(this::mapTilTiltak)
                        .collect(toMap(Tiltakkodeverk::getKode, Tiltakkodeverk::getVerdi))
        );
    }

    public List<Timestamp> hentSluttdatoer(AktorId aktorId) {
        if (aktorId == null) {
            throw new IllegalArgumentException("Trenger aktoerId for å hente ut sluttdatoer");
        }

        final String hentSluttDatoerSql = "SELECT " + TILDATO + " FROM " + TABLE_NAME +
                " WHERE " + AKTOERID + "=?";
        return db.queryForList(hentSluttDatoerSql, Timestamp.class, aktorId.get());
    }


    public List<Timestamp> hentStartDatoer(AktorId aktorId) {
        if (aktorId == null) {
            throw new IllegalArgumentException("Trenger aktoerId for å hente ut startdatoer");
        }
        return db.queryForList("SELECT FRADATO FROM BRUKERTILTAK WHERE AKTOERID = ?",
                Timestamp.class, aktorId.get());
    }

    public List<String> hentBrukertiltak(AktorId aktorIder) {
        if (aktorIder == null) {
            throw new IllegalArgumentException("Trenger aktorid for å hente ut tiltak");
        }
        return db.queryForList("SELECT DISTINCT TILTAKSKODE FROM BRUKERTILTAK WHERE AKTOERID = ?",
                String.class, aktorIder.get());
    }

    public void utledOgLagreTiltakInformasjon(AktorId aktorId) {
        List<BrukertiltakV2> tiltak = hentTiltak(aktorId);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp nesteUtlopsdato = tiltak.stream()
                .map(BrukertiltakV2::getTildato)
                .filter(Objects::nonNull)
                .filter(utlopsdato -> utlopsdato.toLocalDateTime().toLocalDate().isAfter(yesterday))
                .min(Comparator.naturalOrder())
                .orElse(null);

        boolean aktiv = !tiltak.isEmpty();
        AktivitetStatus aktivitetStatus = new AktivitetStatus()
                .setAktivitetType(AktivitetTyper.tiltak.name())
                .setAktiv(aktiv)
                .setAktoerid(aktorId)
                .setNesteUtlop(nesteUtlopsdato);
        aktivitetStatusRepositoryV2.upsertAktivitetTypeStatus(aktivitetStatus, AktivitetTyper.tiltak.name());
    }

    private List<BrukertiltakV2> hentTiltak(AktorId aktorId) {
        String sql = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + AKTOERID + " = ?";
        return db.queryForList(sql, aktorId.get())
                .stream()
                .map(this::mapTilBrukertiltakV2)
                .collect(toList());
    }

    private boolean skalOppdatereTiltakskodeVerk(String tiltaksKode, String verdiFraKafka) {
        Optional<String> verdiITiltakskodeVerk = hentVerdiITiltakskodeVerk(tiltaksKode);
        return verdiITiltakskodeVerk.map(lagretVerdi -> !lagretVerdi.equals(verdiFraKafka)).orElse(true);
    }

    public Optional<String> hentVerdiITiltakskodeVerk(String kode) {
        String sql = "SELECT " + PostgresTable.TILTAKKODEVERK.VERDI + " FROM " + PostgresTable.TILTAKKODEVERK.TABLE_NAME
                + " WHERE " + PostgresTable.TILTAKKODEVERK.KODE + " = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, String.class, kode))
        );
    }


    @SneakyThrows
    private BrukertiltakV2 mapTilBrukertiltakV2(Map<String, Object> rs) {
        return new BrukertiltakV2()
                .setTiltak((String) rs.get(TILTAKSKODE))
                .setTildato((Timestamp) rs.get(TILDATO))
                .setAktorId(AktorId.of((String) rs.get(AKTOERID)));
    }

    @SneakyThrows
    private Tiltakkodeverk mapTilTiltak(Map<String, Object> rs) {
        return Tiltakkodeverk.of((String) rs.get(PostgresTable.TILTAKKODEVERK.KODE), (String) rs.get(PostgresTable.TILTAKKODEVERK.VERDI));
    }
}
