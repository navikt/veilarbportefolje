package no.nav.fo.util.sql;

import no.nav.fo.util.sql.where.WhereClause;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.sql.Date;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpsertQueryTest {
    JdbcTemplate db = mock(JdbcTemplate.class);

    @Test
    public void girEnNogenlundeOkString() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(db.execute(captor.capture(), any(PreparedStatementCallback.class))).thenReturn(true);

        UpsertQuery updateQuery = SqlUtils.upsert(db, "tabellnavn")
                .set("kolonneEn", new Date(0))
                .set("kolonneTo", "Min String")
                .where(WhereClause.equals("kolonnetre", 2131));

        updateQuery.execute();

        assertThat(captor.getValue()).isEqualTo("MERGE INTO tabellnavn USING dual ON (kolonnetre = ?) WHEN MATCHED THEN UPDATE SET kolonneEn = ?, kolonneTo = ? WHEN NOT MATCHED THEN INSERT (kolonneEn, kolonneTo) VALUES (?, ?)");
    }

    @Test
    public void girEnNogenlundeOkString2() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        PreparedStatementCallback<?> psc = mock(PreparedStatementCallback.class);
        when(db.execute(captor.capture(), any(PreparedStatementCallback.class))).thenReturn(true);

        UpsertQuery updateQuery = SqlUtils.upsert(db, "tabellnavn")
                .set("kolonneEn", new Date(0))
                .set("kolonneTo", "Min String")
                .where(WhereClause.equals("kolonnetre", 2131).and(WhereClause.equals("kolonneTo", 1234)));

        updateQuery.execute();

        assertThat(captor.getValue()).isEqualTo("MERGE INTO tabellnavn USING dual ON (kolonnetre = ? AND kolonneTo = ?) WHEN MATCHED THEN UPDATE SET kolonneEn = ?, kolonneTo = ? WHEN NOT MATCHED THEN INSERT (kolonneEn, kolonneTo) VALUES (?, ?)");
    }

    @Test(expected = IllegalStateException.class)
    public void kasterExceptionManIkkeHarParametere() throws Exception {
        UpsertQuery updateQuery = SqlUtils.upsert(db, "tabellnavn")
                .where(WhereClause.equals("kolonnetre", 2131));

        updateQuery.execute();
    }

    @Test(expected = IllegalStateException.class)
    public void kasterExceptionManIkkeHarWhereClause() throws Exception {
        UpsertQuery updateQuery = SqlUtils.upsert(db, "tabellnavn")
                .set("kolonneEn", new Date(0))
                .set("kolonneTo", "Min String");

        updateQuery.execute();
    }
}