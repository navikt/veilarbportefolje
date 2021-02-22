package no.nav.pto.veilarbportefolje.aktiviteter;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.DbUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.database.Table.AKTIVITETER.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parse0OR1;

@Slf4j
@Repository
public class AktivitetDAO {


    private final JdbcTemplate db;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public AktivitetDAO(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public AktorId getAktorId(String aktivitetId) {
        return SqlUtils
                .select(db, Table.AKTIVITETER.TABLE_NAME, rs -> rs.getString(AKTOERID))
                .column(AKTOERID)
                .where(WhereClause.equals(AKTIVITETID, aktivitetId))
                .executeToList()
                .stream()
                .findFirst()
                .map(AktorId::of)
                .orElseThrow(IllegalStateException::new);
    }

    public Long getVersjon(String aktivitetId) {
        return SqlUtils
                .select(db, Table.AKTIVITETER.TABLE_NAME, rs -> rs.getLong(VERSION))
                .column(VERSION)
                .where(WhereClause.equals(AKTIVITETID, aktivitetId))
                .execute();
    }

    public void slettAlleAktivitetstatus(String aktivitettype) {
        db.execute("DELETE FROM BRUKERSTATUS_AKTIVITETER WHERE AKTIVITETTYPE = '" + aktivitettype + "'");
    }

    public AktoerAktiviteter getAktiviteterForAktoerid(AktorId aktoerid) {

        List<AktivitetDTO> queryResult = SqlUtils.select(db,  Table.AKTIVITETER.TABLE_NAME, AktivitetDAO::mapToAktivitetDTO)
                .column(AKTOERID)
                .column(AKTIVITETTYPE)
                .column(STATUS)
                .column(FRADATO)
                .column(TILDATO)
                .where(WhereClause.equals(AKTOERID, aktoerid.toString()))
                .executeToList();

        return new AktoerAktiviteter(aktoerid.toString()).setAktiviteter(queryResult);
    }

    private static AktivitetDTO mapToAktivitetDTO(ResultSet res) throws SQLException {
        return new AktivitetDTO()
                .setAktivitetType(res.getString(AKTIVITETTYPE))
                .setStatus(res.getString(STATUS))
                .setFraDato(res.getTimestamp(FRADATO))
                .setTilDato(res.getTimestamp(TILDATO));
    }

    public void upsertAktivitet(KafkaAktivitetMelding aktivitet) {
        SqlUtils.upsert(db,  Table.AKTIVITETER.TABLE_NAME)
                .set(AKTOERID, aktivitet.getAktorId())
                .set(AKTIVITETTYPE, aktivitet.getAktivitetType().name().toLowerCase())
                .set(AVTALT, aktivitet.isAvtalt())
                .set(FRADATO, toTimestamp(aktivitet.getFraDato()))
                .set(TILDATO, toTimestamp(aktivitet.getTilDato()))
                .set(OPPDATERTDATO, toTimestamp(aktivitet.getEndretDato()))
                .set(STATUS, aktivitet.getAktivitetStatus().name().toLowerCase())
                .set(VERSION, aktivitet.getVersion())
                .set(AKTIVITETID, aktivitet.getAktivitetId())
                .where(WhereClause.equals(AKTIVITETID, aktivitet.getAktivitetId()))
                .execute();
    }

    public void deleteById(String aktivitetid) {
        log.info("Sletter alle aktiviteter med id {}", aktivitetid);
        SqlUtils.delete(db,  Table.AKTIVITETER.TABLE_NAME)
                .where(WhereClause.equals(AKTIVITETID, aktivitetid))
                .execute();
    }

    void upsertAktivitet(Collection<KafkaAktivitetMelding> aktiviteter) {
        aktiviteter.forEach(this::upsertAktivitet);
    }

    public void insertAktivitetstatuser(List<AktivitetStatus> statuser) {
        io.vavr.collection.List.ofAll(statuser).sliding(1000,1000)
                .forEach((statuserBatch) -> {
                    try {
                        AktivitetStatus.batchInsert(db, statuserBatch.toJavaList());
                    } catch (SQLIntegrityConstraintViolationException e) {
                        // Dette oppstår når flere noder prøver å skrive samme data til databasen. Siden alle nodene poller
                        // samtidig er dette en kjent situasjon som oppstår.
                        // TODO: Håndtere dette bedre så ikke nodene prøver å skrive til databasen samtidig
                        log.warn("Status var allerede oppdatert i databasen. Dette kan være en race-condition mellom noder", e);
                    }
                });
    }

    public void insertOrUpdateAktivitetStatus(List<AktivitetStatus> aktivitetStatuses, Collection<Tuple2<PersonId, String>> finnesIdb) {
        Map<Boolean, List<AktivitetStatus>> eksisterendeStatuser = aktivitetStatuses
                .stream()
                .collect(groupingBy((data) -> finnesIdb.contains(Tuple.of(data.getPersonid(), data.getAktivitetType()))));

        AktivitetStatus.batchUpdate(this.db, eksisterendeStatuser.getOrDefault(true, emptyList()));

        insertAktivitetstatuser(eksisterendeStatuser.getOrDefault(false, emptyList()));
    }

    public Map<PersonId, Set<AktivitetStatus>> getAktivitetstatusForBrukere(Collection<PersonId> personIds) {

        if (personIds == null || personIds.isEmpty()) {
            throw new IllegalArgumentException("Trenger person-ider for å hente ut aktivitetsstatuser");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("personids", personIds.stream().map(PersonId::toString).collect(toList()));

        return namedParameterJdbcTemplate
                .queryForList(getAktivitetStatuserForListOfPersonIds(), params)
                .stream()
                .map(AktivitetDAO::mapAktivitetStatus)
                .filter(aktivitetStatus -> AktivitetTyper.contains(aktivitetStatus.getAktivitetType()))
                .collect(toMap(AktivitetStatus::getPersonid, DbUtils::toSet,
                        (oldValue, newValue) -> {
                            oldValue.addAll(newValue);
                            return oldValue;
                        }));
    }

    public List<String> getDistinctAktorIdsFromAktivitet() {
        return db.queryForList("SELECT DISTINCT AKTOERID FROM AKTIVITETER")
                .stream()
                .map(map -> (String) map.get(AKTOERID))
                .collect(toList());

    }

    public List<Brukertiltak> hentBrukertiltak(List<Fnr> fnrs) {

        Map<String, Object> params = new HashMap<>();
        params.put("fnrs", fnrs.stream().map(Fnr::toString).collect(toList()));

        return namedParameterJdbcTemplate
                .queryForList(hentBrukertiltakForListeAvFnrSQL(), params)
                .stream()
                .map(row -> Brukertiltak.of(
                        Fnr.ofValidFnr((String) row.get("FNR")),
                        (String) row.get("TILTAK"),
                        (Timestamp) row.get("TILDATO"))
                )
                .collect(toList());
    }

    private String hentBrukertiltakForListeAvFnrSQL() {
        return "SELECT " +
                "TILTAKSKODE AS TILTAK, " +
                "FODSELSNR as FNR, " +
                "TILDATO " +
                "FROM BRUKERTILTAK " +
                "WHERE FODSELSNR in(:fnrs)";
    }

    private String getAktivitetStatuserForListOfPersonIds() {
        return
                "SELECT " +
                        "PERSONID, " +
                        "AKTOERID, " +
                        "AKTIVITETTYPE, " +
                        "STATUS, " +
                        "NESTE_UTLOPSDATO, " +
                        "NESTE_STARTDATO " +
                        "FROM " +
                        "BRUKERSTATUS_AKTIVITETER " +
                        "WHERE " +
                        "PERSONID in (:personids)";
    }

    public static AktivitetStatus mapAktivitetStatus (Map<String, Object> row) {
        AktorId aktorId = row.get("AKTOERID") == null ? null : AktorId.of((String) row.get("AKTOERID"));
        return new AktivitetStatus()
                .setPersonid(PersonId.of((String) row.get("PERSONID")))
                .setAktoerid(aktorId)
                .setAktivitetType((String) row.get("AKTIVITETTYPE"))
                .setAktiv(parse0OR1((String) row.get("STATUS")))
                .setNesteStart((Timestamp) row.get("NESTE_STARTDATO"))
                .setNesteUtlop((Timestamp) row.get("NESTE_UTLOPSDATO"));
    }

    public boolean erNyVersjonAvAktivitet(KafkaAktivitetMelding aktivitet) {
        Long kommendeVersjon = aktivitet.getVersion();
        if(kommendeVersjon == null){
            return false;
        }
        Long databaseVersjon = getVersjon(aktivitet.getAktivitetId());
        if(databaseVersjon == null ){
            return true;
        }
        return kommendeVersjon.compareTo(databaseVersjon) > 0;
    }

    @Transactional
    public void tryLagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        try {
            if (aktivitet.isHistorisk()) {
                deleteById(aktivitet.getAktivitetId());
            } else if (erNyVersjonAvAktivitet(aktivitet)) {
                upsertAktivitet(aktivitet);
            }
        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata fra topic for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
        }
    }
}
