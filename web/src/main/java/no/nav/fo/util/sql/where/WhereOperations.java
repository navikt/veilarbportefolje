package no.nav.fo.util.sql.where;

public enum WhereOperations {
    EQUALS("="), AND("AND"), OR("OR");

    public final String sql;

    WhereOperations(String sql) {
        this.sql = sql;
    }
}
