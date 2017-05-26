package no.nav.fo.database.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke hsql lokalt med oracle syntax
*/
class HsqlSyntaxMapper {

    private static final Map<String, String> syntaxMap = new HashMap<>();

    static {
        map(
                "INSERT INTO METADATA (SIST_INDEKSERT)\nVALUES (TO_TIMESTAMP('1970-01-01 00:00:00.000000', 'YYYY-MM-DD HH24:MI:SS.FF6'))",
                "INSERT INTO METADATA (SIST_INDEKSERT)\nVALUES (TIMESTAMP '1970-01-01 00:00:00')"
        );
        map(
                "INSERT INTO METADATA (dialogaktor_sist_oppdatert) VALUES (TO_TIMESTAMP('1970-01-01 00:00:00.000000', 'YYYY-MM-DD HH24:MI:SS.FF6'))",
                "INSERT INTO METADATA (dialogaktor_sist_oppdatert) VALUES (TIMESTAMP '1970-01-01 00:00:00')"
        );
    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        if (sql.contains("CREATE MATERIALIZED VIEW") || sql.contains("DROP MATERIALIZED VIEW")) {
            return "SELECT 1 FROM DUAL";
        }
        return syntaxMap.getOrDefault(sql, sql);
    }

}
