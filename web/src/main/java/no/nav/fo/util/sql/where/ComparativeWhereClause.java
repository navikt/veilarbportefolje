package no.nav.fo.util.sql.where;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ComparativeWhereClause extends WhereClause {
    private final WhereOperations operation;
    private final String field;
    private final Object value;

    public ComparativeWhereClause(WhereOperations operation, String field, Object value) {
        this.operation = operation;
        this.field = field;
        this.value = value;
    }

    @Override
    public int applyTo(PreparedStatement ps, int index) throws SQLException {
        ps.setObject(index, this.value);
        return index + 1;
    }

    @Override
    public String toSql() {
        return String.format("%s %s ?", this.field, this.operation.sql);
    }
}
