package no.nav.fo.veilarbportefolje.database;

import no.nav.sbl.sql.SqlUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;

public class MetadataRepository {

    private JdbcTemplate db;
    private final static String METADATA_TABLE_NAME = "METADATA";

    @Inject
    public MetadataRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Timestamp hentDialogerSistOppdatert() {
        return SqlUtils
                .select(db, "METADATA", rs -> rs.getTimestamp("DIALOGAKTOR_SIST_OPPDATERT"))
                .column("DIALOGAKTOR_SIST_OPPDATERT")
                .execute();
    }

    public Timestamp hentAktiviteterSistOppdatert() {
        return SqlUtils
                .select(db, METADATA_TABLE_NAME, rs -> rs.getTimestamp("AKTIVITETER_SIST_OPPDATERT"))
                .column("AKTIVITETER_SIST_OPPDATERT")
                .execute();
    }

    public Timestamp hentOppfolgingstatusSistOppdatert() {
        return SqlUtils
                .select(db, METADATA_TABLE_NAME, rs -> rs.getTimestamp("OPPFOLGING_SIST_OPPDATERT_ID"))
                .column("OPPFOLGING_SIST_OPPDATERT_ID")
                .execute();
    }
}
