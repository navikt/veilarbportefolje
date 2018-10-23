package no.nav.fo.veilarbportefolje.util.sql.where;

public enum WhereOperator {
    EQUALS("="), AND("AND"), OR("OR");

    public final String sql;

    WhereOperator(String sql) {
        this.sql = sql;
    }
}
