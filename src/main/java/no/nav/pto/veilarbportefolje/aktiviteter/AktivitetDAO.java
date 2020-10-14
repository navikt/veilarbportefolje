package no.nav.pto.veilarbportefolje.aktiviteter;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.util.DbUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper.contains;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parse0OR1;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
@Repository
public class AktivitetDAO {

    public static final String AKTIVITETER = "AKTIVITETER";
    private static final String AKTIVITETID = "AKTIVITETID";

    private final JdbcTemplate db;

    @Autowired
    public AktivitetDAO(JdbcTemplate db) {
        this.db = db;
    }

    public AktoerId getAktoerId(String aktivitetId) {
        return SqlUtils
                .select(db, AKTIVITETER, rs -> rs.getString("AKTOERID"))
                .column("AKTOERID")
                .where(WhereClause.equals(AKTIVITETID, aktivitetId))
                .executeToList()
                .stream()
                .findFirst()
                .map(AktoerId::of)
                .orElseThrow(IllegalStateException::new);
    }

    public void slettAlleAktivitetstatus(String aktivitettype) {
        SqlUtils.delete(db, Table.BRUKERSTATUS_AKTIVITETER.TABLE_NAME)
                .where(WhereClause.equals(Table.BRUKERSTATUS_AKTIVITETER.AKTIVITETTYPE, aktivitettype))
                .execute();
    }

    public AktoerAktiviteter getAktiviteterForAktoerid(AktoerId aktoerid) {

        List<AktivitetDTO> queryResult = SqlUtils.select(db, AKTIVITETER, AktivitetDAO::mapToAktivitetDTO)
                .column("AKTOERID")
                .column("AKTIVITETTYPE")
                .column("STATUS")
                .column("FRADATO")
                .column("TILDATO")
                .where(WhereClause.equals("AKTOERID", aktoerid.toString()))
                .executeToList();

        return new AktoerAktiviteter(aktoerid.toString()).setAktiviteter(queryResult);
    }

    private static AktivitetDTO mapToAktivitetDTO(ResultSet res) throws SQLException {
        return new AktivitetDTO()
                .setAktivitetType(res.getString("AKTIVITETTYPE"))
                .setStatus(res.getString("STATUS"))
                .setFraDato(res.getTimestamp("FRADATO"))
                .setTilDato(res.getTimestamp("TILDATO"));
    }

    public void upsertAktivitet(KafkaAktivitetMelding aktivitet) {
        SqlUtils.upsert(db, AKTIVITETER)
                .set("AKTOERID", aktivitet.getAktorId())
                .set("AKTIVITETTYPE", aktivitet.getAktivitetType().name().toLowerCase())
                .set("AVTALT", aktivitet.isAvtalt())
                .set("FRADATO", dateToTimestamp(aktivitet.getFraDato()))
                .set("TILDATO", dateToTimestamp(aktivitet.getTilDato()))
                .set("OPPDATERTDATO", dateToTimestamp(aktivitet.getEndretDato()))
                .set("STATUS", aktivitet.getAktivitetStatus().name().toLowerCase())
                .set(AKTIVITETID, aktivitet.getAktivitetId())
                .where(WhereClause.equals(AKTIVITETID, aktivitet.getAktivitetId()))
                .execute();
    }

    public void deleteById(String aktivitetid) {
        log.info("Sletter alle aktiviteter med id {}", aktivitetid);
        SqlUtils.delete(db, AKTIVITETER)
                .where(WhereClause.equals(AKTIVITETID, aktivitetid))
                .execute();
    }

    void upsertAktivitet(Collection<KafkaAktivitetMelding> aktiviteter) {
        aktiviteter.forEach(this::upsertAktivitet);
    }

    public void insertAktivitetstatuser(List<AktivitetStatus> statuser) {
        io.vavr.collection.List.ofAll(statuser).sliding(1000, 1000)
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

        final List<String> ids = personIds.stream().map(PersonId::toString).collect(toList());

        final List<AktivitetStatus> dtos = SqlUtils.select(db, Table.BRUKERSTATUS_AKTIVITETER.TABLE_NAME, AktivitetDAO::mapAktivitetStatus)
                .column("*")
                .where(in(Table.BRUKERSTATUS_AKTIVITETER.PERSONID, ids))
                .executeToList();

        return dtos
                .stream()
                .filter(aktivitetStatus -> contains(aktivitetStatus.getAktivitetType()))
                .collect(toMap(
                        AktivitetStatus::getPersonid,
                        DbUtils::toSet,
                        (oldValue, newValue) -> {
                            oldValue.addAll(newValue);
                            return oldValue;
                        }));
    }

    public List<String> getDistinctAktoerIdsFromAktivitet() {
        return db.queryForList("SELECT DISTINCT AKTOERID FROM AKTIVITETER")
                .stream()
                .map(map -> (String) map.get("AKTOERID"))
                .collect(toList());

    }

    public List<Brukertiltak> hentBrukertiltak(List<Fnr> fnrs) {
        final List<String> ids = fnrs.stream().map(Fnr::toString).collect(toList());

        return SqlUtils.select(db, Table.BRUKERTILTAK.TABLE_NAME, AktivitetDAO::toBrukerTiltak)
                .column("*")
                .where(in(Table.BRUKERTILTAK.FODSELSNR, ids))
                .executeToList();
    }

    public void slettAktivitetDatoer() {
        SqlUtils.update(db, "bruker_data")
                .set("NYESTEUTLOPTEAKTIVITET", (Object) null)
                .set("AKTIVITET_START", (Object) null)
                .set("NESTE_AKTIVITET_START", (Object) null)
                .set("FORRIGE_AKTIVITET_START", (Object) null)
                .execute();
    }

    @SneakyThrows
    public static AktivitetStatus mapAktivitetStatus(ResultSet resultSet) {
        return new AktivitetStatus()
                .setPersonid(PersonId.of(resultSet.getString("PERSONID")))
                .setAktoerid(AktoerId.of(resultSet.getString("AKTOERID")))
                .setAktivitetType(resultSet.getString("AKTIVITETTYPE"))
                .setAktiv(parse0OR1(resultSet.getString("STATUS")))
                .setNesteStart(resultSet.getTimestamp("NESTE_STARTDATO"))
                .setNesteUtlop(resultSet.getTimestamp("NESTE_UTLOPSDATO"));
    }

    @SneakyThrows
    private static Brukertiltak toBrukerTiltak(ResultSet rs) {
        return Brukertiltak.of(
                Fnr.of(rs.getString(Table.BRUKERTILTAK.FODSELSNR)),
                rs.getString(Table.BRUKERTILTAK.TILTAKSKODE),
                rs.getTimestamp(Table.BRUKERTILTAK.TILDATO));
    }
}
