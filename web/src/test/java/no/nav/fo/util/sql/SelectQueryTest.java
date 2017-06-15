package no.nav.fo.util.sql;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;

public class SelectQueryTest {

    private JdbcTemplate db = mock(JdbcTemplate.class);

    @Test
    public void shouldGenerateValidSelectQueryString() throws Exception {
        String expectedQueryString = "select COLUMN_1, COLUMN_2 from TABLE_NAME where SOME_ID = ?";

        String actualQueryString = SqlUtils.select(db, "TABLE_NAME")
                .column("COLUMN_1")
                .column("COLUMN_2")
                .whereEquals("SOME_ID", 123)
                .toString();

        Assert.assertEquals(expectedQueryString, actualQueryString);
    }
}
