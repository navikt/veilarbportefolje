package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.PROSSESERT_AKTIVIT_DATA.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProssesertAktivitetRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsertAktivitetStatus(AktivitetStatus status) {
        if (status.getAktoerid() == null || status.getAktivitetType() == null) {
            log.warn("Kunne ikke lagre aktivitet pga. null verdier");
            return;
        }
        // TODO: update syntax
        SqlUtils.upsert(db, TABLE_NAME)
                .set(AKTOERID, status.getAktoerid().get())
                .set(AKTIVITETTYPE, status.getAktivitetType())
                .set(AKTIV, status.isAktiv())
                .set(NESTE_UTLOPSDATO, status.getNesteUtlop())
                .set(NESTE_STARTDATO, status.getNesteStart())
                .where(WhereClause.equals(AKTOERID, status.getAktoerid().get())
                        .and(WhereClause.equals(AKTIVITETTYPE, status.getAktivitetType())))
                .execute();
    }
}
