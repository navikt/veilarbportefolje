package no.nav.fo.veilarbportefolje.util.sql;

import no.nav.fo.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.fo.veilarbportefolje.util.sql.where.WhereClause;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class DeleteQueryTest {

    @Inject
    private DataSource ds;

    @Test
    public void skalGenereRiktigSqlStreng() throws Exception {
        String expected = "DELETE FROM TABLE_NAME WHERE FOO = ?";
        String actual = SqlUtils.delete(ds, "TABLE_NAME")
                .where(WhereClause.equals("FOO", 1))
                .toString();

        assertEquals(expected, actual);
    }
}
