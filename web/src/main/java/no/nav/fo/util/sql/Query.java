package no.nav.fo.util.sql;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Query {
    private Connection connection;
    private StringBuilder stringBuilder;
    private final String SEPARATOR = ", ";

    @SneakyThrows
    public static Query of(DataSource ds) {
        Query query = new Query();
        query.connection = ds.getConnection();
        query.stringBuilder = new StringBuilder();
        return query;
    }

    public Query select(String... columnNames) {
        Stream.of(columnNames)
                .flatMap(x -> Stream.of(SEPARATOR, x))
                .skip(1)
                .forEach(stringBuilder::append);
        return this;
    }

    public Query selectAll() {
        stringBuilder.append("*");
        return this;
    }

    public Query from(String... tableNames) {
        Stream.of(tableNames)
                .flatMap(x -> Stream.of(SEPARATOR, x))
                .skip(1)
                .forEach(stringBuilder::append);
        return this;
    }

    public Query where(String whereClause) {
        stringBuilder.append(whereClause);
        return this;
    }

    private Optional<ResultSet> execute() {
        Optional<ResultSet> maybeResult;
        try {
            maybeResult = Optional.of(connection.prepareStatement(stringBuilder.toString()).executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return maybeResult;
    }

    @SneakyThrows
    public <T> Stream<T> fetch(Function<ResultSet, T> mapper) {
        ResultSet rs = this.execute().orElseThrow(RuntimeException::new);
        return new ResultSetIterable<>(rs, mapper).stream();
    }

}