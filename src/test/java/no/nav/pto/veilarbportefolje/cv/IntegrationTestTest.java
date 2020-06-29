package no.nav.pto.veilarbportefolje.cv;

import no.nav.sbl.sql.SqlUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;

import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_DATA.PERSONID;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_DATA.TABLE_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/*
Test to see if the in-memory database is cleared between running each test method
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntegrationTestTest extends IntegrationTest {

    @Test
    public void testA() {
        SqlUtils
                .insert(jdbcTemplate, TABLE_NAME)
                .value(PERSONID, "1")
                .execute();
    }

    @Test
    public void testB() {
        List<String> list = SqlUtils.select(jdbcTemplate, TABLE_NAME, rs -> rs.getString(PERSONID))
                .column("*")
                .executeToList();
        assertThat(list.size()).isEqualTo(0);
    }
}
