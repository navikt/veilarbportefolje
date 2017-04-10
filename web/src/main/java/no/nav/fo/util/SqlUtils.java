package no.nav.fo.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
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
