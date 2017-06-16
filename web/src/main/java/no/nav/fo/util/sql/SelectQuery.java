package no.nav.fo.util.sql;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class SelectQuery<T> {
    private JdbcTemplate db;
    private String tableName;
    private String whereParam;
    private Object whereValue;
    private List<String> columnNames;
    private Function<ResultSet, T> mapper;

    public SelectQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.columnNames = new ArrayList<>();
    }

    public SelectQuery<T> column(String columnName) {
        this.columnNames.add(columnName);
        return this;
    }

    public SelectQuery<T> whereEquals(String whereParam, Object whereValue) {
        this.whereParam = whereParam;
        this.whereValue = whereValue;
        return this;
    }

    public Optional<T> execute() {
        assert tableName != null;

        String sql = createSqlString();
        return db.query(sql, createSqlArgumentArray(), rs -> rs.next() ? Optional.of(mapper.apply(rs)) : Optional.empty());
    }

    private String createSqlString() {
        StringBuilder sqlBuilder = new StringBuilder()
                .append("select ");

        columnNames.stream()
                .flatMap(x -> Stream.of(", ", x))
                .skip(1)
                .forEach(sqlBuilder::append);

        sqlBuilder
                .append(" ")
                .append("from ")
                .append(tableName);

        if (this.whereParam != null) {
            sqlBuilder.append(" where ").append(whereParam).append(" = ?");
        }

        return sqlBuilder.toString();
    }

    public SelectQuery<T> usingMapper(Function<ResultSet, T> mapper) {
        this.mapper = mapper;
        return this;
    }

    private Object[] createSqlArgumentArray() {
        if (whereValue == null) {
            return new Object[]{};
        }
        Object[] arguments = new Object[1];
        int index = 0;
        arguments[index] = whereValue;
        return arguments;
    }

    @Override
    public String toString() {
        return createSqlString();
    }

}
