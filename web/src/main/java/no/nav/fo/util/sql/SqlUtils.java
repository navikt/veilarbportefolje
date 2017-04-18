package no.nav.fo.util.sql;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Function;

public class SqlUtils {
    static Function<String, String> append(final String suffix) {
        return (String value) -> value + suffix;
    }

    public static UpdateQuery update(JdbcTemplate db, String tableName) {
        return new UpdateQuery(db, tableName);
    }

    public static <S> UpdateBatchQuery<S> updateBatch(JdbcTemplate db, String tableName, Class<S> cls) {
        return new UpdateBatchQuery<>(db, tableName);
    }

    public static InsertQuery insert(JdbcTemplate db, String tableName) {
        return new InsertQuery(db, tableName);
    }
}
