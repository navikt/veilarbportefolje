package no.nav.fo.util.sql;

import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class UpsertQuery {
    private final String upsertTemplate = "MERGE INTO %s USING dual ON (%s) WHEN MATCHED THEN %s WHEN NOT MATCHED THEN %s";
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Object> setParams;
    private final Map<String, Object> insertParams;
    private WhereClause where;

    public UpsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
        this.insertParams = new LinkedHashMap<>();
    }

    public UpsertQuery set(String param, Object value) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, value);
        return this;
    }

    public UpsertQuery insert(String param, Object value) {
        if (this.insertParams.containsKey(param)) {
            throw new IllegalArgumentException(format("Param[%s] was already set.", param));
        }
        this.insertParams.put(param, value);
        return this;
    }



    public UpsertQuery where(WhereClause where) {
        this.where = where;
        return this;
    }

    public void execute() {
        if (this.where == null || this.setParams.isEmpty()) {
            throw new IllegalStateException("Invalid data");
        }

        db.execute(createUpsertStatement(), (PreparedStatementCallback<Boolean>) ps -> {
            int index = 1;

            index = this.where.applyTo(ps, index);

            // For updatequery
            for (Map.Entry<String, Object> entry : this.setParams.entrySet()) {
                ps.setObject(index++, entry.getValue());
            }

            // For insertquery
            for (Map.Entry<String, Object> entry : this.insertParams.entrySet()) {
                ps.setObject(index++, entry.getValue());
            }

            return ps.execute();
        });
    }

    private String createUpsertStatement() {
        return format(
                upsertTemplate,
                this.tableName,
                this.where.toSql(),
                this.createUpdateStatement(),
                this.createInsertStatement()
        );
    }

    private String createUpdateStatement() {
        return format("UPDATE SET %s", this.createSetStatement());
    }

    private String createSetStatement() {
        return setParams
                .entrySet().stream()
                .map(Map.Entry::getKey)
                .map(SqlUtils.append(" = ?"))
                .collect(joining(", "));
    }

    private String createInsertStatement() {
        return format("INSERT (%s) VALUES (%s)", this.createInsertFields(), this.createInsertValues());
    }

    private String createInsertFields() {
        return insertParams
                .entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(joining(", "));
    }

    private String createInsertValues() {
        return insertParams
                .entrySet().stream()
                .map((entry) -> "?")
                .collect(joining(", "));
    }
}
