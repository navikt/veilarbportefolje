package no.nav.fo.veilarbportefolje.util.sql;

import no.nav.fo.veilarbportefolje.util.sql.where.WhereClause;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;

import static org.mockito.Mockito.mock;

public class SelectQueryTest {

    private DataSource ds = mock(JdbcTemplate.class).getDataSource();

    @Test
    public void shouldGenerateValidSelectQueryString() throws Exception {
        String expectedQueryString = "SELECT COLUMN_1, COLUMN_2 FROM TABLE_NAME WHERE SOME_ID = ?";

        String actualQueryString = SqlUtils.select(ds, "TABLE_NAME", this::mapper)
                .column("COLUMN_1")
                .column("COLUMN_2")
                .where(WhereClause.equals("SOME_ID", 123))
                .toString();

        Assert.assertEquals(expectedQueryString, actualQueryString);
    }

    private String mapper(ResultSet resultSet) {
        return "";
    }
}
