package no.nav.fo.veilarbportefolje.util.sql;

import lombok.SneakyThrows;
import no.nav.fo.veilarbportefolje.util.DbUtils;
import no.nav.fo.veilarbportefolje.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;

import static no.nav.sbl.dialogarena.common.abac.pep.Utils.timed;

public class DeleteQuery {
    private final JdbcTemplate ds;
    private final String tableName;
    private WhereClause where;

    DeleteQuery(JdbcTemplate ds, String tableName) {
        this.ds = ds;
        this.tableName = tableName;
    }

    public DeleteQuery where(WhereClause where) {
        this.where = where;
        return this;
    }

    @SneakyThrows
    public int execute() {
        if (tableName == null || this.where == null) {
            throw new SqlUtilsException(
                    "I need more data to create a sql-statement. " +
                            "Did you remember to specify table and a where clause?"
            );
        }

        String sql = createDeleteStatement();

        return timed(DbUtils.dbTimerNavn(sql), () -> ds.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            where.applyTo(preparedStatement, 1);
            return preparedStatement;
        }));
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
