package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetSchedueldDTO;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.arenaaktiviteter.ArenaAktivitetUtils.getDateOrNull;
import static no.nav.pto.veilarbportefolje.database.Table.GRUPPE_AKTIVITER.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;


@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class GruppeAktivitetRepository {
    private final JdbcTemplate db;
    private final AktivitetDAO aktivitetDAO;

    public void upsertGruppeAktivitet(GruppeAktivitetInnhold gruppeAktivitet, AktorId aktorId, boolean aktiv) {
        // Fra dato kan ha verdien null, det tilsier at aktiviteten varer en hel dag
        ZonedDateTime tilDato = getDateOrNull(gruppeAktivitet.getAktivitetperiodeTil(), true);
        ZonedDateTime fraDato = gruppeAktivitet.getAktivitetperiodeFra() == null ? tilDato.minusDays(1) : getDateOrNull(gruppeAktivitet.getAktivitetperiodeFra());
        String aktivChar = aktiv ? "J" : "N";

        SqlUtils.upsert(db, TABLE_NAME)
                .set(MOTEPLAN_ID, gruppeAktivitet.getMoteplanId())
                .set(VEILEDNINGDELTAKER_ID, gruppeAktivitet.getVeiledningdeltakerId())
                .set(AKTOERID, aktorId.get())
                .set(MOTEPLAN_STARTDATO, toTimestamp(fraDato))
                .set(MOTEPLAN_SLUTTDATO, toTimestamp(tilDato))
                .set(HENDELSE_ID, gruppeAktivitet.getHendelseId())
                .set(AKTIV, aktivChar)
                .where(
                        WhereClause.equals(MOTEPLAN_ID, gruppeAktivitet.getMoteplanId())
                                .and(WhereClause.equals(VEILEDNINGDELTAKER_ID, gruppeAktivitet.getVeiledningdeltakerId()))
                ).execute();
        utledOgLagreGruppeaktiviteter(PersonId.of(String.valueOf(gruppeAktivitet.getPersonId())), aktorId);
    }

    /**
     * Setter aktivitet til inaktiv kun hvis aktiviteten er utgatt.
     * Implementert til aa forhindre race condition mellom daglig jobb og kafka.
     */
    public int oppdaterUtgattAktivStatus(long moteplanId, long veiledningdeltakerId, AktorId aktorId, PersonId personId) {
        String updateSql = String.format(
                "UPDATE %s SET %s = 'N' WHERE %s = ? AND %s = ? AND %s < CURRENT_TIMESTAMP",
                TABLE_NAME, AKTIV, MOTEPLAN_ID, VEILEDNINGDELTAKER_ID, MOTEPLAN_SLUTTDATO
        );

        int updated = db.update(updateSql, moteplanId, veiledningdeltakerId);
        if (updated != 0) {
            utledOgLagreGruppeaktiviteter(personId, aktorId);
        }
        return updated;
    }

    public Optional<Long> retrieveHendelse(GruppeAktivitetInnhold aktivitet) {
        return Optional.ofNullable(
                SqlUtils.select(db, TABLE_NAME, rs -> rs.getLong(HENDELSE_ID))
                        .column(HENDELSE_ID)
                        .where(
                                WhereClause.equals(MOTEPLAN_ID, aktivitet.getMoteplanId())
                                        .and(WhereClause.equals(VEILEDNINGDELTAKER_ID, aktivitet.getVeiledningdeltakerId()))
                        )
                        .execute()
        );
    }

    public Optional<GruppeAktivitetSchedueldDTO> hentAktivtet(long moteplanId, long veiledningdeltakerId) {
        return Optional.ofNullable(
                SqlUtils.select(db, TABLE_NAME, this::mapTilDto)
                        .column("*")
                        .where(WhereClause.equals(MOTEPLAN_ID, moteplanId)
                                .and(WhereClause.equals(VEILEDNINGDELTAKER_ID, veiledningdeltakerId))
                        ).execute()
        );
    }

    public List<GruppeAktivitetSchedueldDTO> hentUtgatteAktivteter() {
        String sql = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + MOTEPLAN_SLUTTDATO + " < CURRENT_TIMESTAMP";
        return db.queryForList(sql)
                .stream()
                .map(result -> (ResultSet) result)
                .map(this::mapTilDto)
                .collect(toList());
    }

    private void utledOgLagreGruppeaktiviteter(PersonId personId, AktorId aktorId) {
        List<GruppeAktivitetSchedueldDTO> gruppeAktiviteter = hentAktiveAktivteter();
        Timestamp nesteStart = gruppeAktiviteter.stream()
                .filter(GruppeAktivitetSchedueldDTO::isAktiv)
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeFra)
                .min(Comparator.naturalOrder())
                .orElse(null);
        Timestamp nesteUtlopsdato = gruppeAktiviteter.stream()
                .filter(GruppeAktivitetSchedueldDTO::isAktiv)
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeFra)
                .max(Comparator.naturalOrder())
                .orElse(null);

        boolean aktiv = (nesteStart != null && nesteUtlopsdato !=null);
        AktivitetStatus aktivitetStatus = new AktivitetStatus()
                .setAktivitetType(AktivitetTyper.gruppeaktivitet.name())
                .setAktiv(aktiv)
                .setAktoerid(aktorId)
                .setPersonid(personId)
                .setNesteStart(nesteStart)
                .setNesteUtlop(nesteUtlopsdato);
        aktivitetDAO.upsertAktivitetStatus(aktivitetStatus);
    }

    private List<GruppeAktivitetSchedueldDTO> hentAktiveAktivteter() {
        String sql = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + AKTIV + " = 'J'";
        return db.queryForList(sql)
                .stream()
                .map(this::mapTilDto)
                .collect(toList());
    }

    @SneakyThrows
    private GruppeAktivitetSchedueldDTO mapTilDto(ResultSet rs) {
        boolean aktiv = "J".equals(rs.getString(AKTIV));
        return new GruppeAktivitetSchedueldDTO()
                .setVeiledningdeltakerId(rs.getLong(VEILEDNINGDELTAKER_ID))
                .setAktivitetperiodeFra(rs.getTimestamp(MOTEPLAN_STARTDATO))
                .setAktivitetperiodeTil(rs.getTimestamp(MOTEPLAN_SLUTTDATO))
                .setMoteplanId(rs.getInt(MOTEPLAN_ID))
                .setHendelseId(rs.getLong(HENDELSE_ID))
                .setAktorId(AktorId.of(AKTOERID))
                .setAktiv(aktiv);
    }

    @SneakyThrows
    private GruppeAktivitetSchedueldDTO mapTilDto(Map<String, Object> rs) {
        boolean aktiv = "J".equals(rs.get(AKTIV));
        return new GruppeAktivitetSchedueldDTO()
                .setVeiledningdeltakerId((Long) rs.get(VEILEDNINGDELTAKER_ID))
                .setAktivitetperiodeFra((Timestamp) rs.get(MOTEPLAN_STARTDATO))
                .setAktivitetperiodeTil((Timestamp) rs.get(MOTEPLAN_SLUTTDATO))
                .setMoteplanId((Integer) rs.get(MOTEPLAN_ID))
                .setHendelseId((Integer) rs.get(HENDELSE_ID))
                .setAktorId(AktorId.of(AKTOERID))
                .setAktiv(aktiv);
    }
}
