package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.BrukertiltakV2;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.DbUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKERTILTAK_V2.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV2 {
    private final JdbcTemplate db;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final AktivitetDAO aktivitetDAO;

    public void upsert(Brukertiltak tiltak, AktorId aktorId, String aktivitetId) {
        log.info("Lagrer tiltak: {}", aktivitetId);
        SqlUtils.upsert(db, TABLE_NAME)
                .set(AKTIVITETID, aktivitetId)
                .set(AKTOERID, aktorId.get())
                .set(TILTAKSKODE, tiltak.getTiltak())
                .set(TILDATO, tiltak.getTildato())
                .where(WhereClause.equals(AKTIVITETID, aktivitetId))
                .execute();
    }

    public void delete(String tiltakId) {
        log.info("Sletter tiltak: {}", tiltakId);
        SqlUtils.delete(db, TABLE_NAME)
                .where(WhereClause.equals(AKTIVITETID, tiltakId))
                .execute();
    }

    private List<BrukertiltakV2> hentTiltak(AktorId aktorId) {
        String sql = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + AKTOERID + " = ?";
        return db.queryForList(sql, aktorId.get())
                .stream()
                .map(this::mapTilBrukertiltakV2)
                .collect(toList());
    }

    public void utledOgLagreTiltakInformasjon(PersonId personId, AktorId aktorId) {
        List<BrukertiltakV2> gruppeAktiviteter = hentTiltak(aktorId);
        Timestamp nesteUtlopsdato = gruppeAktiviteter.stream()
                .map(BrukertiltakV2::getTildato)
                .max(Comparator.naturalOrder())
                .orElse(null);

        boolean aktiv = (nesteUtlopsdato != null);
        AktivitetStatus aktivitetStatus = new AktivitetStatus()
                .setAktivitetType(AktivitetTyper.tiltak.name())
                .setAktiv(aktiv)
                .setAktoerid(aktorId)
                .setPersonid(personId)
                .setNesteUtlop(nesteUtlopsdato);
        aktivitetDAO.upsertAktivitetStatus(aktivitetStatus);
    }

    /*
    Kan forenkles til kun en bruker ved overgang til postgres
    String sql = "SELECT DISTINCT " + Table.BRUKERTILTAK_V2.TILTAKSKODE + " FROM " + Table.BRUKERTILTAK_V2.TABLE_NAME
                + " WHERE " + Table.BRUKERTILTAK_V2.AKTOERID + " = ?";
     */
    public Map<AktorId, Set<BrukertiltakV2>> hentBrukertiltak(List<AktorId> aktorIder) {
        if (aktorIder == null || aktorIder.isEmpty()) {
            throw new IllegalArgumentException("Trenger aktor-ider for å hente ut tiltak");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("aktorIder", aktorIder.stream().map(AktorId::toString).collect(toList()));

        return namedParameterJdbcTemplate
                .queryForList(getTiltaksTyperForListOfAktorIds(), params)
                .stream()
                .map(this::mapTilBrukertiltakV2)
                .collect(toMap(BrukertiltakV2::getAktorId, DbUtils::toSet,
                        (oldValue, newValue) -> {
                            oldValue.addAll(newValue);
                            return oldValue;
                        }));
    }

    private String getTiltaksTyperForListOfAktorIds() {
        return
                "SELECT " +
                        "TILTAKSKODE, " +
                        "AKTOERID, " +
                        "TILDATO" +
                        "FROM " +
                        "BRUKERTILTAK_V2 " +
                        "WHERE " +
                        "AKTOERID in (:aktorIder)";
    }

    private String getTiltaksTyperPaEnhet() {
        return "SELECT DISTINCT TILTAKSKODE FROM" +
                " BRUKERTILTAK_V2" +
                " INNER JOIN VEILARBPORTEFOLJE.OPPFOLGINGSBRUKER OP ON BRUKERTILTAK_V2."+PERSONID+" = OP.PERSON_ID" +
                " WHERE OP.NAV_KONTOR=?";
    }

    @SneakyThrows
    private BrukertiltakV2 mapTilBrukertiltakV2(Map<String, Object> rs) {
        return new BrukertiltakV2()
                .setTiltak((String) rs.get(TILTAKSKODE))
                .setTildato((Timestamp) rs.get(TILDATO))
                .setAktorId(AktorId.of((String) rs.get(AKTOERID)));
    }
}
