package no.nav.fo.database;

import org.apache.solr.common.SolrInputDocument;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.DbUtils.mapResultSetTilDokument;

public class BrukerRepository {

    @Inject
    private JdbcTemplate db;

    public List<SolrInputDocument> retrieveAlleBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return brukere.stream().filter(this::skalBrukerHaOppfolging).collect(toList());
    }

    public List<SolrInputDocument> retrieveOppdaterteBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveOppdaterteBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return brukere.stream().filter(this::skalBrukerHaOppfolging).collect(toList());
    }

    public int updateTidsstempel(Timestamp tidsstempel) {
        return db.update(updateTidsstempelSQL(), tidsstempel);
    }

    String retrieveBrukereSQL() {
        return
                "SELECT " +
                    "person_id, " +
                    "fodselsnr, " +
                    "fornavn, " +
                    "etternavn, " +
                    "nav_kontor, " +
                    "formidlingsgruppekode, " +
                    "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS') || 'Z' AS iserv_fra_dato, " +
                    "kvalifiseringsgruppekode, " +
                    "rettighetsgruppekode, " +
                    "hovedmaalkode, " +
                    "sikkerhetstiltak_type_kode, " +
                    "fr_kode, " +
                    "sperret_ansatt, " +
                    "er_doed, " +
                    "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS') || 'Z' AS doed_fra_dato, " +
                    "tidsstempel " +
                "FROM " +
                    "oppfolgingsbruker";
    }

    String retrieveOppdaterteBrukereSQL() {
        return
                "SELECT " +
                    "person_id, " +
                    "fodselsnr, " +
                    "fornavn, " +
                    "etternavn, " +
                    "nav_kontor, " +
                    "formidlingsgruppekode, " +
                    "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS') || 'Z' AS iserv_fra_dato, " +
                    "kvalifiseringsgruppekode, " +
                    "rettighetsgruppekode, " +
                    "hovedmaalkode, " +
                    "sikkerhetstiltak_type_kode, " +
                    "fr_kode, " +
                    "sperret_ansatt, " +
                    "er_doed, " +
                    "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS') || 'Z' AS doed_fra_dato, " +
                    "tidsstempel " +
                "FROM " +
                    "oppfolgingsbruker " +
                "WHERE " +
                    "tidsstempel > (" + retrieveSistIndeksertSQL() + ")";
    }

    String retrieveSistIndeksertSQL() {
        return "SELECT SIST_INDEKSERT FROM METADATA";
    }

    String updateTidsstempelSQL() {
        return
                "UPDATE METADATA SET SIST_INDEKSERT = ?";
    }

    private boolean skalBrukerHaOppfolging(SolrInputDocument bruker) {
        if (bruker.get("formidlingsgruppekode").getValue().equals("ISERV")) {
            return false;
        }
        return true;
    }
}
