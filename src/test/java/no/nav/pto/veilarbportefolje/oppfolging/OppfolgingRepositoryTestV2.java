package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingRepositoryTestV2 {

    private JdbcTemplate db;
    private OppfolgingRepositoryV2 oppfolgingRepository;

    @Before
    public void setup() {
        db =  SingletonPostgresContainer.init().createJdbcTemplate();;
        oppfolgingRepository = new OppfolgingRepositoryV2(db);
    }

    @Test
    public void skal_sette_oppfolging_til_false() {
        final AktorId aktoerId = AktorId.of("0");

        SqlUtils.insert(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, true)
                .execute();

        oppfolgingRepository.settOppfolgingTilFalse(aktoerId);

        final String oppfolging = SqlUtils.select(db, Table.OPPFOLGING_DATA.TABLE_NAME, rs -> rs.getString(Table.OPPFOLGING_DATA.OPPFOLGING))
                .column(Table.OPPFOLGING_DATA.OPPFOLGING)
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString()))
                .execute();

        assertThat(oppfolging);
    }

}
