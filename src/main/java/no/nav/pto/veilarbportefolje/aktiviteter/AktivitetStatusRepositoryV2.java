package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETTYPE_STATUS.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AktivitetStatusRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsertAktivitetStatus(AktivitetStatus status) {
        if (status.getAktoerid() == null || status.getAktivitetType() == null) {
            log.warn("Kunne ikke lagre aktivitet pga. null verdier");
            return;
        }

        db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTOERID + ", " + AKTIVITETTYPE + ", " + AKTIV + ", " + NESTE_UTLOPSDATO + ", " + NESTE_STARTDATO + ") " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ") " +
                        "DO UPDATE SET (" + AKTIVITETTYPE + ", " + AKTIV + ", " + NESTE_UTLOPSDATO + ", " + NESTE_STARTDATO + ") = (?, ?, ?, ?)",
                status.getAktoerid(),
                status.getAktivitetType().toLowerCase(), status.aktiv, status.getNesteUtlop(), status.getNesteStart(),
                status.getAktivitetType().toLowerCase(), status.aktiv, status.getNesteUtlop(), status.getNesteStart()
        );
    }

    public void upsertAktivitetData(Brukerdata status) {
        if (status.getAktoerid() == null) {
            log.warn("Kunne ikke lagre aktivitet status pga. null verdier");
            return;
        }

        db.update("INSERT INTO " + PostgresTable.AKTIVITET_STATUS.TABLE_NAME +
                        " (" + PostgresTable.AKTIVITET_STATUS.AKTOERID + ", " + PostgresTable.AKTIVITET_STATUS.NYESTEUTLOPTEAKTIVITET + ", " + PostgresTable.AKTIVITET_STATUS.FORRIGE_AKTIVITET_START + ", " + PostgresTable.AKTIVITET_STATUS.AKTIVITET_START + ", " + PostgresTable.AKTIVITET_STATUS.NESTE_AKTIVITET_START + ") " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + PostgresTable.AKTIVITET_STATUS.AKTOERID + ") " +
                        "DO UPDATE SET (" +  PostgresTable.AKTIVITET_STATUS.NYESTEUTLOPTEAKTIVITET + ", " + PostgresTable.AKTIVITET_STATUS.FORRIGE_AKTIVITET_START + ", " + PostgresTable.AKTIVITET_STATUS.AKTIVITET_START + ", " + PostgresTable.AKTIVITET_STATUS.NESTE_AKTIVITET_START + ") = (?, ?, ?, ?)",
                status.getAktoerid(),
                status.getNyesteUtlopteAktivitet(), status.getForrigeAktivitetStart(), status.getAktivitetStart(), status.getNesteAktivitetStart(),
                status.getNyesteUtlopteAktivitet(), status.getForrigeAktivitetStart(), status.getAktivitetStart(), status.getNesteAktivitetStart()
        );

    }
}
