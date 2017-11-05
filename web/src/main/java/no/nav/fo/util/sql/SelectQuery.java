package no.nav.fo.util.sql;

import lombok.SneakyThrows;
import no.nav.fo.util.DbUtils;
import no.nav.fo.util.sql.where.WhereClause;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static no.nav.sbl.dialogarena.common.abac.pep.Utils.timed;

public class SelectQuery<T> {
    private DataSource ds;
    private String tableName;
    private List<String> columnNames;
    private Function<ResultSet, T> mapper;
    private WhereClause where;

    SelectQuery(DataSource ds, String tableName, Function<ResultSet, T> mapper) {
        this.ds = ds;
        this.tableName = tableName;
        this.columnNames = new ArrayList<>();
        this.mapper = mapper;
    }

    public SelectQuery<T> column(String columnName) {
        this.columnNames.add(columnName);
        return this;
    }

    public SelectQuery<T> where(WhereClause where) {
        this.where = where;
        return this;
    }

    @SneakyThrows
    public T execute() {
        validate();
        String sql = createSelectStatement();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            where.applyTo(ps, 1);
            ResultSet resultSet = timed(DbUtils.dbTimerNavn(sql), ps::executeQuery);
            if(!resultSet.next()) {
                return null;
            }
            return mapper.apply(resultSet);

        } catch (SQLException e) {
            throw new SqlUtilsException(e);
        }
    }

    private void validate() {
        if (tableName == null || columnNames.isEmpty()|| this.where == null) {
            throw new SqlUtilsException(
                    "I need more data to create a sql-statement. " +
                    "Did you remember to specify table, columns or a where clause?"
            );
        }

        if (mapper == null) {
            throw new SqlUtilsException("I need a mapper function in order to return the right data type.");
        }
    }

    private String createSelectStatement() {
        StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT ");

        columnNames.stream()
                .flatMap(x -> Stream.of(", ", x))
                .skip(1)
                .forEach(sqlBuilder::append);

        sqlBuilder
                .append(" ")
                .append("FROM ")
                .append(tableName)
                .append(" WHERE ");

        sqlBuilder
                .append(this.where.toSql());

        return sqlBuilder.toString();
    }

    @Override
    public String toString() {
        return createSelectStatement();
    }

}
