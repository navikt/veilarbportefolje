package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetSchedueldDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.AKTIV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.HENDELSE_ID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.MOTEPLAN_ID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.MOTEPLAN_SLUTTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.MOTEPLAN_STARTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.VEILEDNINGDELTAKER_ID;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;


@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class GruppeAktivitetRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;
    private final AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2;

    public void upsertGruppeAktivitet(GruppeAktivitetInnhold aktivitet, AktorId aktorId, boolean aktiv) {
        // Fra dato kan ha verdien null, det tilsier at aktiviteten varer en hel dag
        LocalDateTime tilDato = getLocalDateTimeOrNull(aktivitet.getAktivitetperiodeTil(), true);
        LocalDateTime fraDato = Optional.ofNullable(getLocalDateTimeOrNull(aktivitet.getAktivitetperiodeFra(), false))
                .orElse(LocalDateTime.of(tilDato.toLocalDate(), LocalTime.MIDNIGHT));

        db.update("INSERT INTO " + TABLE_NAME +
                        " (" + MOTEPLAN_ID + ", " + VEILEDNINGDELTAKER_ID + ", " + AKTOERID + ", " + MOTEPLAN_STARTDATO + ", " + MOTEPLAN_SLUTTDATO + ", " + HENDELSE_ID + ", " + AKTIV + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + MOTEPLAN_ID + ", " + VEILEDNINGDELTAKER_ID + ") " +
                        "DO UPDATE SET (" + AKTOERID + ", " + MOTEPLAN_STARTDATO + ", " + MOTEPLAN_SLUTTDATO + ", " + HENDELSE_ID + ", " + AKTIV + ") = (?, ?, ?, ?, ?)",
                aktivitet.getMoteplanId(), aktivitet.getVeiledningdeltakerId(),
                aktorId.get(), fraDato, tilDato, aktivitet.getHendelseId(), aktiv,
                aktorId.get(), fraDato, tilDato, aktivitet.getHendelseId(), aktiv
        );
    }

    public Optional<Long> retrieveHendelse(GruppeAktivitetInnhold aktivitet) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? AND  %s = ?", TABLE_NAME, MOTEPLAN_ID, VEILEDNINGDELTAKER_ID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getLong(HENDELSE_ID), aktivitet.getMoteplanId(), aktivitet.getVeiledningdeltakerId()))
        );
    }

    public void utledOgLagreGruppeaktiviteter(AktorId aktorId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<GruppeAktivitetSchedueldDTO> gruppeAktiviteter = hentAktiveAktivteter(aktorId);
        Timestamp nesteStart = gruppeAktiviteter.stream()
                .filter(GruppeAktivitetSchedueldDTO::isAktiv)
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeFra)
                .filter(startDato -> startDato.toLocalDateTime().toLocalDate().isAfter(yesterday))
                .min(Comparator.naturalOrder())
                .orElse(null);
        Timestamp nesteUtlopsdato = gruppeAktiviteter.stream()
                .filter(GruppeAktivitetSchedueldDTO::isAktiv)
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeTil)
                .filter(utlopsDato -> utlopsDato.toLocalDateTime().toLocalDate().isAfter(yesterday))
                .min(Comparator.naturalOrder())
                .orElse(null);

        boolean aktiv = (nesteUtlopsdato != null
                && gruppeAktiviteter.stream().anyMatch(GruppeAktivitetSchedueldDTO::isAktiv));
        AktivitetStatus aktivitetStatus = new AktivitetStatus()
                .setAktivitetType(AktivitetTyper.gruppeaktivitet.name())
                .setAktiv(aktiv)
                .setAktoerid(aktorId)
                .setNesteStart(nesteStart)
                .setNesteUtlop(nesteUtlopsdato);
        aktivitetStatusRepositoryV2.upsertAktivitetTypeStatus(aktivitetStatus, AktivitetTyper.gruppeaktivitet.name());
    }

    public List<GruppeAktivitetSchedueldDTO> hentAktiveAktivteter(AktorId aktorId) {
        String sql = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + AKTIV + " AND " + AKTOERID + " = ?";
        return db.queryForList(sql, aktorId.get())
                .stream()
                .map(this::mapTilDto)
                .collect(toList());
    }

    @SneakyThrows
    private GruppeAktivitetSchedueldDTO mapTilDto(Map<String, Object> rs) {
        long hendelse = Optional.ofNullable((Long) rs.get(HENDELSE_ID)).orElse(0L);
        return new GruppeAktivitetSchedueldDTO()
                .setVeiledningdeltakerId((String) rs.get(VEILEDNINGDELTAKER_ID))
                .setMoteplanId((String) rs.get(MOTEPLAN_ID))
                .setAktivitetperiodeFra((Timestamp) rs.get(MOTEPLAN_STARTDATO))
                .setAktivitetperiodeTil((Timestamp) rs.get(MOTEPLAN_SLUTTDATO))
                .setHendelseId(hendelse)
                .setAktorId(AktorId.of(AKTOERID))
                .setAktiv((Boolean) rs.get(AKTIV));
    }
}
