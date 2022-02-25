package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.database.Table.TILTAKKODEVERK_V2;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.Tiltakkodeverk;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getDateOrNull;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.AKTIVITETID;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.FRADATO;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.PERSONID;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.TILDATO;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.TILTAKSKODE;
import static no.nav.pto.veilarbportefolje.database.Table.OPPFOLGINGSBRUKER.PERSON_ID;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV1 {
    private final JdbcTemplate db;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void upsert(TiltakInnhold innhold, AktorId aktorId) {
        Timestamp tilDato = Optional.ofNullable(getDateOrNull(innhold.getAktivitetperiodeTil(), true))
                .map(DateUtils::toTimestamp)
                .orElse(null);
        Timestamp fraDato = Optional.ofNullable(getDateOrNull(innhold.getAktivitetperiodeFra(), false))
                .map(DateUtils::toTimestamp)
                .orElse(null);

        log.info("Lagrer tiltak: {}", innhold.getAktivitetid());

        if (skalOppdatereTiltakskodeVerk(innhold.getTiltakstype(), innhold.getTiltaksnavn())) {
            SqlUtils.upsert(db, TILTAKKODEVERK_V2.TABLE_NAME)
                    .set(TILTAKKODEVERK_V2.KODE, innhold.getTiltakstype())
                    .set(TILTAKKODEVERK_V2.VERDI, innhold.getTiltaksnavn())
                    .where(WhereClause.equals(TILTAKKODEVERK_V2.KODE, innhold.getTiltakstype()))
                    .execute();
        }
        SqlUtils.upsert(db, TABLE_NAME)
                .set(AKTIVITETID, innhold.getAktivitetid())
                .set(PERSONID, String.valueOf(innhold.getPersonId()))
                .set(AKTOERID, aktorId.get())
                .set(TILTAKSKODE, innhold.getTiltakstype())
                .set(FRADATO, fraDato)
                .set(TILDATO, tilDato)
                .where(WhereClause.equals(AKTIVITETID, innhold.getAktivitetid()))
                .execute();

    }

    public void delete(String tiltakId) {
        log.info("Sletter tiltak: {}", tiltakId);
        SqlUtils.delete(db, TABLE_NAME)
                .where(WhereClause.equals(AKTIVITETID, tiltakId))
                .execute();
    }

    public EnhetTiltak hentTiltakPaEnhet(EnhetId enhetId) {
        final String hentTiltakPaEnhetSql = "SELECT * FROM " + TILTAKKODEVERK_V2.TABLE_NAME + " WHERE " +
                TILTAKKODEVERK_V2.KODE + " IN (SELECT DISTINCT " + TILTAKSKODE + " FROM " + BRUKERTILTAK_V2.TABLE_NAME +
                " BT INNER JOIN " + Table.OPPFOLGINGSBRUKER.TABLE_NAME + " OP ON BT." + PERSONID + " = OP." + PERSON_ID +
                " WHERE OP.NAV_KONTOR=:nav_kontor)";

        return new EnhetTiltak().setTiltak(
                namedParameterJdbcTemplate
                        .queryForList(hentTiltakPaEnhetSql, Map.of("nav_kontor", enhetId.get()))
                        .stream().map(this::mapTilTiltak)
                        .collect(toMap(Tiltakkodeverk::getKode, Tiltakkodeverk::getVerdi))
        );
    }

    private boolean skalOppdatereTiltakskodeVerk(String tiltaksKode, String verdiFraKafka) {
        Optional<String> verdiITiltakskodeVerk = hentVerdiITiltakskodeVerk(tiltaksKode);
        return verdiITiltakskodeVerk.map(lagretVerdi -> !lagretVerdi.equals(verdiFraKafka)).orElse(true);
    }

    private Optional<String> hentVerdiITiltakskodeVerk(String kode) {
        String sql = "SELECT " + TILTAKKODEVERK_V2.VERDI + " FROM " + TILTAKKODEVERK_V2.TABLE_NAME
                + " WHERE " + TILTAKKODEVERK_V2.KODE + " = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, String.class, kode))
        );
    }

    @SneakyThrows
    private Tiltakkodeverk mapTilTiltak(Map<String, Object> rs) {
        return Tiltakkodeverk.of((String) rs.get(TILTAKKODEVERK_V2.KODE), (String) rs.get(TILTAKKODEVERK_V2.VERDI));
    }
}
