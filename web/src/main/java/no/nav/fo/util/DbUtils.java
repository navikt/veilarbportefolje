package no.nav.fo.util;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.fo.util.DateUtils.toIsoUTC;

public class DbUtils {

    private static Logger logger = LoggerFactory.getLogger(DbUtils.class);

    public static SolrInputDocument mapResultSetTilDokument(ResultSet rs) {
        try {
            return mapTilDokument(rs);
        }catch(SQLException e) {
            logger.error("Feil ved mapping til SolrIntputDocument", e);
            return null;
        }
    }

    private static SolrInputDocument mapTilDokument(ResultSet rs) throws SQLException {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("person_id", numberToString(rs.getBigDecimal("person_id")));
        document.addField("fnr", rs.getString("fodselsnr"));
        document.addField("fornavn", kapitaliser( rs.getString( "fornavn")));
        document.addField("etternavn", kapitaliser(rs.getString("etternavn")));
        document.addField("enhet_id", rs.getString("nav_kontor"));
        document.addField("formidlingsgruppekode", rs.getString("formidlingsgruppekode"));
        document.addField("iserv_fra_dato", toIsoUTC(rs.getTimestamp("iserv_fra_dato")));
        document.addField("kvalifiseringsgruppekode", rs.getString("kvalifiseringsgruppekode"));
        document.addField("rettighetsgruppekode", rs.getString("rettighetsgruppekode"));
        document.addField("hovedmaalkode", rs.getString("hovedmaalkode"));
        document.addField("sikkerhetstiltak", rs.getString("sikkerhetstiltak_type_kode"));
        document.addField("diskresjonskode", rs.getString("fr_kode"));
        document.addField("egen_ansatt", parseJaNei(rs.getString("sperret_ansatt"), "sperret_ansatt"));
        document.addField("er_doed", parseJaNei(rs.getString("er_doed"), "er_doed"));
        document.addField("doed_fra_dato", toIsoUTC(rs.getTimestamp("doed_fra_dato")));
        document.addField("veileder_id", rs.getString("veilederident"));
        document.addField("fodselsdag_i_mnd", FodselsnummerUtils.lagFodselsdagIMnd(rs.getString("fodselsnr")));
        document.addField("fodselsdato", FodselsnummerUtils.lagFodselsdato(rs.getString("fodselsnr")));
        document.addField("kjonn", FodselsnummerUtils.lagKjonn(rs.getString("fodselsnr")));
        document.addField("ytelse", rs.getString("ytelse"));
        document.addField("utlopsdato", toIsoUTC(rs.getTimestamp("utlopsdato")));
        document.addField("utlopsdatofasett", rs.getString("utlopsdatofasett"));
        document.addField("dagputlopuke", parseInt(rs.getString("dagputlopuke")));
        document.addField("dagputlopukefasett", rs.getString("dagputlopukefasett"));
        document.addField("permutlopuke", parseInt(rs.getString("permutlopuke")));
        document.addField("permutlopukefasett", rs.getString("permutlopukefasett"));
        document.addField("aapmaxtiduke", parseInt(rs.getString("aapmaxtiduke")));
        document.addField("aapmaxtidukefasett", rs.getString("aapmaxtidukefasett"));
        document.addField("oppfolging", parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING"));
        document.addField("venterpasvarfrabruker", toIsoUTC(rs.getTimestamp("venterpasvarfrabruker")));
        document.addField("venterpasvarfranav", toIsoUTC(rs.getTimestamp("venterpasvarfranav")));
        document.addField("nyesteutlopteaktivitet", toIsoUTC(rs.getTimestamp("nyesteutlopteaktivitet")));
        document.addField("iavtaltaktivitet", parse0OR1(rs.getString("iavtaltaktivitet")));

        return document;
    }

    static String kapitaliser(String s) {
        return WordUtils.capitalizeFully(s, ' ', '\'', '-');
    }

    public static boolean parseJaNei(Object janei, String name) {
        boolean defaultValue  = false;
        if (janei == null) {
            logger.debug(String.format("%s er ikke satt i databasen, defaulter til %b", name, defaultValue));
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

    static Integer parseInt(String integer) {
        if (integer == null) {
            return null;
        }
        return Integer.parseInt(integer);
    }

    public static Boolean parse0OR1(String value) {
        if(value == null) {
            return null;
        }
        return "1".equals(value);
    }

    public static String numberToString(BigDecimal bd) {
        return String.valueOf(bd.intValue());
    }

    public static String getCauseString(Throwable e) {
        if (e.getCause() == null) {
            return e.getMessage();

        }
        return e.getCause().toString();
    }
}
