package no.nav.fo.util;

import org.apache.solr.common.SolrInputDocument;

import java.util.Map;

public class DbUtils {

    public static SolrInputDocument mapRadTilDokument(Map<String, Object> rad) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("person_id", rad.get("person_id").toString());
        document.addField("fodselsnr", rad.get("fodselsnr").toString());
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
        document.addField("egen_ansatt", parseJaNei(rad.get("sperret_ansatt")));
        document.addField("er_doed", parseJaNei("er_doed"));
        document.addField("doed_fra_dato", parseDato(rad.get("doed_fra_dato")));
        document.addField("veileder_id", null);
        return document;
    }

    public static String parseDato(Object dato) {
        if (dato == null) {
            return null;
        } else if (dato.equals("TZ")) {
            return null;
        } else {
            return dato.toString();
        }
    }

    public static boolean parseJaNei(Object janei) {
        switch (janei.toString()) {
            case "J":
                return true;
            case "N":
                return false;
            default:
                throw new RuntimeException(String.format("Kunne ikke parse verdi %s fra database til boolean", janei));
        }
    }
}
