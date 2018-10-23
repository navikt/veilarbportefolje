package no.nav.fo.veilarbportefolje.util.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ResultSetIterable<T> implements Iterable<T> {

    private ResultSet rs;
    private Function<ResultSet, T> onNext;

    public ResultSetIterable(ResultSet rs, Function<ResultSet, T> onNext) {
        this.rs = rs;
        this.onNext = onNext;
    }

    public Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    @Override
    public Iterator<T> iterator() {
        try {
            return new Iterator<T>() {
                boolean hasNext = rs.next();

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public T next() {
                    T result = onNext.apply(rs);

                    try {
                        hasNext = rs.next();
                    } catch (SQLException e) {
                        try {
                            rs.close();
                        } catch (SQLException e1) {
                            throw new RuntimeException(e);
                        }
                        throw new RuntimeException(e);
                    }
                    return result;
                }
            };
        } catch (SQLException e) {
            try {
                rs.close();
            } catch (SQLException e1) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException(e);
        }
    }
}
