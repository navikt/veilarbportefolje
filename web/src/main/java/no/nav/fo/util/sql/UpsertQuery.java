package no.nav.fo.util.sql;

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
    private String whereParam;
    private Object whereValue;

    public UpsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
    }

    public UpsertQuery set(String param, Object value) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, value);
        return this;
    }

    public UpsertQuery whereEquals(String whereParam, Object whereValue) {
        this.whereParam = whereParam;
        this.whereValue = whereValue;
        return this;
    }

    public void execute() {
        if (this.whereParam == null || this.whereValue == null || this.setParams.isEmpty()) {
            throw new IllegalStateException("Invalid data");
        }

        db.execute(createUpsertStatement(), (PreparedStatementCallback<Boolean>) ps -> {
            int index = 1;
            ps.setObject(index++, this.whereValue);

            // For updatequery
            for (Map.Entry<String, Object> entry : this.setParams.entrySet()) {
                ps.setObject(index++, entry.getValue());
            }

            // For insertquery
            for (Map.Entry<String, Object> entry : this.setParams.entrySet()) {
                ps.setObject(index++, entry.getValue());
            }

            return ps.execute();
        });
    }

    private String createUpsertStatement() {
        return format(
                upsertTemplate,
                this.tableName,
                this.createWhereStatement(),
                this.createUpdateStatement(),
                this.createInsertStatement()
        );
    }

    private String createWhereStatement() {
        return format("%s = ?", this.whereParam);
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
        return setParams
                .entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(joining(", "));
    }

    private String createInsertValues() {
        return setParams
                .entrySet().stream()
                .map((entry) -> "?")
                .collect(joining(", "));
    }
}
