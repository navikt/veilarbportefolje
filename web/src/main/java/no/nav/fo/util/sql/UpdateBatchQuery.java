package no.nav.fo.util.sql;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static no.nav.fo.util.DbUtils.dbTimerNavn;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class UpdateBatchQuery<T> {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Tuple2<Class, Function<T, Object>>> setParams;
    private Function<T, WhereClause> whereClause;

    public UpdateBatchQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
    }

    public UpdateBatchQuery<T> add(String param, Function<T, Object> paramValue, Class type) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, Tuple.of(type, paramValue));
        return this;
    }

    public UpdateBatchQuery<T> addWhereClause(Function<T, WhereClause> whereClause) {
        this.whereClause = whereClause;
        return this;
    }

    public int[] execute(List<T> data) {
        if (data.isEmpty()) {
            return null;
        }
        String sql = createSql(data.get(0));
        return timed(dbTimerNavn(sql), ()-> db.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                T t = data.get(i);

                int sqlParamIndex = 1;

                for (Map.Entry<String, Tuple2<Class, Function<T, Object>>> paramEntry : setParams.entrySet()) {
                    String paramname = paramEntry.getKey();
                    Tuple2<Class, Function<T, Object>> param = paramEntry.getValue();

                    setParam(ps, sqlParamIndex++, paramname, param._1(), param._2.apply(t));
                }

                if(Objects.nonNull(whereClause)) {
                    whereClause.apply(t).applyTo(ps, sqlParamIndex);
                }

            }

            @Override
            public int getBatchSize() {
                return data.size();
            }
        }));
    }

    static void setParam(PreparedStatement ps, int i, String paramname, Class type, Object value) throws SQLException {
    try {
        if (String.class == type) {
            ps.setString(i, (String) value);
        } else if (Timestamp.class == type) {
            ps.setTimestamp(i, (Timestamp) value);
        } else if (Integer.class == type) {
            if (value != null) {
                ps.setInt(i, (Integer) value);
            } else {
                ps.setInt(i, -1);
            }
        } else if (Boolean.class == type) {
            ps.setBoolean(i, (Boolean) value);
        }
    } catch (Exception e) {
        log.error(format("Feil ved binding av %s", paramname), e);
        throw e;
        }
    }

    String createSql(T t) {
        StringBuilder sqlBuilder = new StringBuilder()
                .append("update ").append(tableName)
                .append(createSetStatement());

        if (Objects.nonNull(whereClause)) {
            sqlBuilder.append(" where ").append(whereClause.apply(t).toSql());
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
