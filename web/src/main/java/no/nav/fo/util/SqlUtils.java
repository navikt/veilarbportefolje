package no.nav.fo.util;

import javaslang.Tuple;
import javaslang.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

public class SqlUtils {
    public static final class InsertQuery {
        private final JdbcTemplate db;
        private final String tableName;
        private final Map<String, Object> insertParams;

        public InsertQuery(JdbcTemplate db, String tableName) {
            this.db = db;
            this.tableName = tableName;
            this.insertParams = new LinkedHashMap<>();
        }

        public static InsertQuery insert(JdbcTemplate db, String tableName) {
            return new InsertQuery(db, tableName);
        }

        public InsertQuery value(String columnName, Object value) {
            this.insertParams.put(columnName, value);
            return this;
        }

        public void execute() {
            db.update(createSqlStatement(), insertParams.values().toArray());
        }

        private String createSqlStatement() {
            String columns = StringUtils.join(insertParams.keySet(), ",");
            String values = StringUtils.join(nCopies(insertParams.size(), "?"), ",");
            return String.format("insert into %s (%s) values (%s)", tableName, columns, values);
        }
    }

    public static final class UpdateBatchQuery<T> {
        private final JdbcTemplate db;
        private final String tableName;
        private final Map<String, Tuple2<Class, Function<T, Object>>> setParams;
        private String whereParam;
        private Function<T, String> whereValue;

        public UpdateBatchQuery(JdbcTemplate db, String tableName) {
            this.db = db;
            this.tableName = tableName;
            this.setParams = new LinkedHashMap<>();
        }

        public static <S> UpdateBatchQuery<S> updateBatch(JdbcTemplate db, String tableName, Class<S> cls) {
            return new UpdateBatchQuery(db, tableName);
        }

        public UpdateBatchQuery<T> add(String param, Function<T, Object> paramValue, Class type) {
            if (this.setParams.containsKey(param)) {
                throw new IllegalArgumentException(String.format("Param[%s] was already set.", param));
            }
            this.setParams.put(param, Tuple.of(type, paramValue));
            return this;
        }

        public UpdateBatchQuery<T> addWhereClause(String param, Function<T, String> paramValue) {
            this.whereParam = param;
            this.whereValue = paramValue;
            return this;
        }

        public int[] execute(List<T> data) {
            if (data.isEmpty()) {
                return null;
            }

            return db.batchUpdate(createSql(), new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    T t = data.get(i);

                    int j = 1;
                    for (Tuple2<Class, Function<T, Object>> param : setParams.values()) {
                        setParam(ps, j++, param._1(), param._2.apply(t));
                    }

                    if (StringUtils.isNotBlank(whereParam)) {
                        setParam(ps, j++, String.class, whereValue.apply(t));
                    }
                }

                @Override
                public int getBatchSize() {
                    return data.size();
                }
            });
        }

        static void setParam(PreparedStatement ps, int i, Class type, Object value) throws SQLException {
            if (String.class == type) {
                ps.setString(i, (String) value);
            } else if (Timestamp.class == type) {
                ps.setTimestamp(i, (Timestamp) value);
            }
        }

        String createSql() {
            StringBuilder sqlBuilder = new StringBuilder()
                    .append("update ").append(tableName)
                    .append(createSetStatement());

            if (this.whereParam != null) {
                sqlBuilder.append(" where ").append(whereParam).append(" = ?");
            }

            return sqlBuilder.toString();
        }

        private String createSetStatement() {
            return " SET " + setParams
                    .keySet()
                    .stream()
                    .map(append(" = ?"))
                    .collect(joining(", "));
        }
    }


    public static final class UpdateQuery {
        private final JdbcTemplate db;
        private final String tableName;
        private final Map<String, Object> setParams;
        private String whereParam;
        private Object whereValue;

        private UpdateQuery(JdbcTemplate db, String tableName) {
            this.db = db;
            this.tableName = tableName;
            this.setParams = new LinkedHashMap<>();
        }

        public static UpdateQuery update(JdbcTemplate db, String tableName) {
            return new UpdateQuery(db, tableName);
        }

        public UpdateQuery set(String param, Object value) {
            if (this.setParams.containsKey(param)) {
                throw new IllegalArgumentException(String.format("Param[%s] was already set.", param));
            }
            this.setParams.put(param, value);
            return this;
        }

        public UpdateQuery whereEquals(String whereParam, Object whereValue) {
            this.whereParam = whereParam;
            this.whereValue = whereValue;
            return this;
        }

        public void execute() {
            assert tableName != null;
            assert !setParams.isEmpty();

            StringBuilder sqlBuilder = new StringBuilder()
                    .append("update ").append(tableName)
                    .append(createSetStatement());

            if (this.whereParam != null) {
                sqlBuilder.append(" where ").append(whereParam).append(" = ?");
            }

            String sql = sqlBuilder.toString();

            db.update(sql, createSqlArgumentArray());
        }

        private Object[] createSqlArgumentArray() {
            int argumentsSize = whereValue != null ? setParams.size() + 1 : setParams.size();
            Object[] arguments = new Object[argumentsSize];

            int index = 0;
            for (Object value : setParams.values()) {
                arguments[index] = value;
                index = index + 1;
            }
            if (whereValue != null) {
                arguments[index] = whereValue;
            }

            return arguments;
        }

        private String createSetStatement() {
            return " set " + setParams.entrySet().stream()
                    .map(entry -> entry.getKey())
                    .map(append(" = ?"))
                    .collect(joining(", "));
        }

    }

    private static Function<String, String> append(final String suffix) {
        return (String value) -> value + suffix;
    }
}
