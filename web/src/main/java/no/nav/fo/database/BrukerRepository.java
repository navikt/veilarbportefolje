package no.nav.fo.database;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class BrukerRepository {

    @Inject
    private JdbcTemplate db;

    public List<Map<String, Object>> retrieveAlleBrukere() {
        List<Map<String, Object>> rader = db.queryForList(retrieveBrukereSQL());
        return rader;
    }

    public List<Map<String, Object>> retrieveNyeBrukere() {
        List<Map<String, Object>> rader = db.queryForList(retrieveNyeBrukereSQL());
        return rader;
    }

    public void updateTidsstempel(Timestamp tidsstempel) {
        db.update(updateTidsstempelSQL(), tidsstempel);
    }

    private String retrieveBrukereSQL() {
        return
                "SELECT " +
                    "person_id, " +
                    "fodselsnr, " +
                    "fornavn, " +
                    "etternavn, " +
                    "nav_kontor, " +
                    "formidlingsgruppekode, " +
                    "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS.FF') || 'Z' AS iserv_fra_dato, " +
                    "kvalifiseringsgruppekode, " +
                    "rettighetsgruppekode, " +
                    "hovedmaalkode, " +
                    "sikkerhetstiltak_type_kode, " +
                    "fr_kode, " +
                    "sperret_ansatt, " +
                    "er_doed, " +
                    "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS.FF') || 'Z' AS doed_fra_dato " +
                "FROM " +
                    "oppfolgingsbruker";
    }

    private String retrieveNyeBrukereSQL() {
        return
                "SELECT " +
                    "person_id, " +
                    "fodselsnr, " +
                    "fornavn, " +
                    "etternavn, " +
                    "nav_kontor, " +
                    "formidlingsgruppekode, " +
                    "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS.FF') || 'Z' AS iserv_fra_dato, " +
                    "kvalifiseringsgruppekode, " +
                    "rettighetsgruppekode, " +
                    "hovedmaalkode, " +
                    "sikkerhetstiltak_type_kode, " +
                    "fr_kode, " +
                    "sperret_ansatt, " +
                    "er_doed, " +
                    "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS.FF') || 'Z' AS doed_fra_dato, " +
                    "tidsstempel " +
                "FROM " +
                    "oppfolgingsbruker " +
                "WHERE " +
                    "tidsstempel > (SELECT sist_indeksert FROM indeksering_logg)";
    }

    private String updateTidsstempelSQL() {
        return
                "UPDATE indeksering_logg SET sist_indeksert = ?";
    }
}
