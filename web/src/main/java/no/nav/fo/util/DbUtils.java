package no.nav.fo.util;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class DbUtils {

    private static Logger logger = LoggerFactory.getLogger(DbUtils.class);

    public static SolrInputDocument mapResultSetTilDokument(ResultSet rs) {
        SolrInputDocument document = new SolrInputDocument();
        try {
            document.addField("person_id", rs.getString("person_id"));
            document.addField("fnr", rs.getString("fodselsnr"));
            document.addField("fornavn", rs.getString("fornavn"));
            document.addField("etternavn", rs.getString("etternavn"));
            document.addField("enhet_id", rs.getString("nav_kontor"));
            document.addField("formidlingsgruppekode", rs.getString("formidlingsgruppekode"));
            document.addField("iserv_fra_dato", parseDato(rs.getString("iserv_fra_dato")));
            document.addField("kvalifiseringsgruppekode", rs.getString("kvalifiseringsgruppekode"));
            document.addField("rettighetsgruppekode", rs.getString("rettighetsgruppekode"));
            document.addField("hovedmaalkode", rs.getString("hovedmaalkode"));
            document.addField("sikkerhetstiltak", rs.getString("sikkerhetstiltak_type_kode"));
            document.addField("diskresjonskode", rs.getString("fr_kode"));
            document.addField("egen_ansatt", parseJaNei(rs.getString("sperret_ansatt"), "sperret_ansatt"));
            document.addField("er_doed", parseJaNei(rs.getString("er_doed"), "er_doed"));
            document.addField("doed_fra_dato", parseDato(rs.getString("doed_fra_dato")));
            document.addField("veileder_id", null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return document;
    }


    public static SolrInputDocument mapRadTilDokument(Map<String, Object> rad) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("person_id", rad.get("person_id").toString());
        document.addField("fnr", rad.get("fodselsnr").toString());
        document.addField("fornavn", rad.get("fornavn").toString());
        document.addField("etternavn", rad.get("etternavn").toString());
        document.addField("enhet_id", rad.get("nav_kontor").toString());
        document.addField("formidlingsgruppekode", rad.get("formidlingsgruppekode").toString());
        document.addField("iserv_fra_dato", parseDato(rad.get("iserv_fra_dato")));
        document.addField("kvalifiseringsgruppekode", rad.get("kvalifiseringsgruppekode").toString());
        document.addField("rettighetsgruppekode", rad.get("rettighetsgruppekode").toString());
        document.addField("hovedmaalkode", rad.get("hovedmaalkode") != null ? rad.get("hovedmaalkode").toString() : null);
        document.addField("sikkerhetstiltak", rad.get("sikkerhetstiltak_type_kode") != null ? rad.get("sikkerhetstiltak_type_kode").toString() : null);
        document.addField("diskresjonskode", rad.get("fr_kode") != null ? rad.get("fr_kode").toString() : null);
        document.addField("egen_ansatt", parseJaNei(rad.get("sperret_ansatt"), "sperret_ansatt"));
        document.addField("er_doed", parseJaNei(rad.get("er_doed"), "er_doed"));
        document.addField("doed_fra_dato", parseDato(rad.get("doed_fra_dato")));
        document.addField("veileder_id", null);
        return document;
    }

    static String parseDato(Object dato) {
        if (dato == null) {
            return null;
        } else if (dato.equals("TZ")) {
            return null;
        } else {
            return dato.toString();
        }
    }

    static boolean parseJaNei(Object janei, String name) {
        boolean defaultValue  = true;
        if (janei == null) {
            logger.warn(String.format("%s er ikke satt i databasen, defaulter til ", defaultValue));
            return defaultValue;
        }

        switch (janei.toString()) {
            case "J":
                return true;
            case "N":
                return false;
            default:
                throw new IllegalArgumentException(String.format("Kunne ikke parse verdi %s fra database til boolean", janei));
        }
    }
}
