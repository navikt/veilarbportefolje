package no.nav.fo.util.sql.where;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class WhereClause {
    public static WhereClause equals(String field, Object value) {
        return new ComparativeWhereClause(WhereOperator.EQUALS, field, value);
    }

    public WhereClause and(WhereClause other) {
        return new LogicalWhereClause(WhereOperator.AND, this, other);
    }

    public WhereClause or(WhereClause other) {
        return new LogicalWhereClause(WhereOperator.OR, this, other);
    }

    public abstract int applyTo(PreparedStatement ps, int index) throws SQLException;

    public abstract String toSql();

    public abstract boolean appliesTo(String key);
}
