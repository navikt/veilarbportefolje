package no.nav.fo.veilarbportefolje.util.sql;

public class SqlUtilEmptyResultSetException extends RuntimeException{
    public SqlUtilEmptyResultSetException(String sql) {
        super("Følgende sql ga tomt ResultSet: " + sql);
    }
}
