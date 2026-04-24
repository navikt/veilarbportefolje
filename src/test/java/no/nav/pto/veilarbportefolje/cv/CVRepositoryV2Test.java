package no.nav.pto.veilarbportefolje.cv;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_REGISTRERT_CV.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static org.assertj.core.api.Assertions.assertThat;

public class CVRepositoryV2Test {

    private JdbcTemplate db;
    private CVRepositoryV2 cvRepositoryV2;

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        cvRepositoryV2 = new CVRepositoryV2(db);
    }

    @Test
    public void skal_teste_om_bruker_CV_eksisterer_i_database() {
        Fnr fnr = Fnr.of("12345678901");

        cvRepositoryV2.upsertCvRegistrert(fnr, null, true);
        assertThat(cvEksisterer(fnr)).isTrue();

        cvRepositoryV2.upsertCvRegistrert(fnr, null, false);
        assertThat(cvEksisterer(fnr)).isFalse();
    }

    @Test
    public void skal_teste_om_bruker_ble_slettet_fra_cv_databasen() {
        Fnr fnr = Fnr.of("12345678901");

        cvRepositoryV2.upsertCvRegistrert(fnr, null, true);
        assertThat(cvEksisterer(fnr)).isTrue();

        cvRepositoryV2.slettCvRegistrert(fnr);
        assertThat(cvEksisterer(fnr)).isFalse();
    }

    private boolean cvEksisterer(Fnr fnr) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", CV_EKSISTERER, TABLE_NAME, FNR);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(CV_EKSISTERER), fnr.get()))
        ).orElse(false);
    }
}
