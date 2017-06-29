package no.nav.fo.util.sql;

import no.nav.fo.util.sql.where.WhereClause;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteQuery {
    private final DataSource ds;
    private final String tableName;
    private WhereClause where;

    DeleteQuery(DataSource ds, String tableName) {
        this.ds = ds;
        this.tableName = tableName;
    }

    public DeleteQuery where(WhereClause where) {
        this.where = where;
        return this;
    }

    public int execute() {
        if (tableName == null || this.where == null) {
            throw new IllegalStateException("Invalid data");
        }

        int result;
        try {
            Connection conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(createDeleteStatement());
            where.applyTo(ps, 1);

            result = ps.executeUpdate();
            if (result == 0) {
                throw new RuntimeException("Could not delete row");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private String createDeleteStatement() {
        return String.format(
                "DELETE FROM %s WHERE %s",
                tableName,
                this.where.toSql()
        );
    }

    @Override
    public String toString() {
        return createDeleteStatement();
    }
}
