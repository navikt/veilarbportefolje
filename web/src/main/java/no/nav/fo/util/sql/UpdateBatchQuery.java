package no.nav.fo.util.sql;

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

import static java.util.stream.Collectors.joining;

public class UpdateBatchQuery<T> {
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
        } else if (Boolean.class == type) {
            ps.setBoolean(i, (Boolean) value);
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
                .map(SqlUtils.append(" = ?"))
                .collect(joining(", "));
    }
}
