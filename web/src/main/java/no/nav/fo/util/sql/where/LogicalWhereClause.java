package no.nav.fo.util.sql.where;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LogicalWhereClause extends WhereClause {
    private final WhereOperations operation;
    private final WhereClause wc1;
    private final WhereClause wc2;

    public LogicalWhereClause(WhereOperations operation, WhereClause wc1, WhereClause wc2) {
        this.operation = operation;
        this.wc1 = wc1;
        this.wc2 = wc2;

    }

    @Override
    public int applyTo(PreparedStatement ps, int index) throws SQLException {
        int i = this.wc1.applyTo(ps, index);
        return this.wc2.applyTo(ps, i);
    }

    @Override
    public String toSql() {
        return String.format("%s %s %s", this.wc1.toSql(), this.operation.sql, this.wc2.toSql());
    }
}
