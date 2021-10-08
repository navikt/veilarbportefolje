package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETTYPE_STATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITET_STATUS;

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

        db.update("INSERT INTO " + AKTIVITETTYPE_STATUS.TABLE_NAME +
                        " (" + AKTIVITETTYPE_STATUS.AKTOERID + ", " + AKTIVITETTYPE_STATUS.AKTIVITETTYPE + ", " + AKTIVITETTYPE_STATUS.AKTIV + ", " + AKTIVITETTYPE_STATUS.NESTE_UTLOPSDATO + ", " + AKTIVITETTYPE_STATUS.NESTE_STARTDATO + ") " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTIVITETTYPE_STATUS.AKTOERID + ") " +
                        "DO UPDATE SET (" + AKTIVITETTYPE_STATUS.AKTIVITETTYPE + ", " + AKTIVITETTYPE_STATUS.AKTIV + ", " +AKTIVITETTYPE_STATUS.NESTE_UTLOPSDATO + ", " + AKTIVITETTYPE_STATUS.NESTE_STARTDATO + ") = (?, ?, ?, ?)",
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

        db.update("INSERT INTO " + AKTIVITET_STATUS.TABLE_NAME +
                        " (" + AKTIVITET_STATUS.AKTOERID + ", " + AKTIVITET_STATUS.NYESTEUTLOPTEAKTIVITET + ", " + AKTIVITET_STATUS.FORRIGE_AKTIVITET_START + ", " + AKTIVITET_STATUS.AKTIVITET_START + ", " + AKTIVITET_STATUS.NESTE_AKTIVITET_START + ") " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTIVITET_STATUS.AKTOERID + ") " +
                        "DO UPDATE SET (" +  AKTIVITET_STATUS.NYESTEUTLOPTEAKTIVITET + ", " + AKTIVITET_STATUS.FORRIGE_AKTIVITET_START + ", " + AKTIVITET_STATUS.AKTIVITET_START + ", " + AKTIVITET_STATUS.NESTE_AKTIVITET_START + ") = (?, ?, ?, ?)",
                status.getAktoerid(),
                status.getNyesteUtlopteAktivitet(), status.getForrigeAktivitetStart(), status.getAktivitetStart(), status.getNesteAktivitetStart(),
                status.getNyesteUtlopteAktivitet(), status.getForrigeAktivitetStart(), status.getAktivitetStart(), status.getNesteAktivitetStart()
        );

    }
}
