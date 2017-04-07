package no.nav.fo.util;

import lombok.SneakyThrows;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.Map;
import java.util.function.BiFunction;

public class DbUtils {

    private static Logger logger = LoggerFactory.getLogger(DbUtils.class);

    @SneakyThrows
    private static String resultsetGetter(ResultSet rs, String key) {
        return rs.getString(key);
    }

    private static String mapGetter(Map<String, Object> rs, String key) {
        Object o = rs.get(key);

        if (o == null) {
            return null;
        }

        if (o instanceof Number) {
            return String.valueOf(((Number) o).intValue());
        }
        return o.toString();
    }

    public static SolrInputDocument mapResultSetTilDokument(ResultSet rs) {
        return mapTilDokument(rs, DbUtils::resultsetGetter);
    }

    public static SolrInputDocument mapRadTilDokument(Map<String, Object> map) {
        return mapTilDokument(map, DbUtils::mapGetter);
    }

    private static <T> SolrInputDocument mapTilDokument(T rs, BiFunction<T, String, String> fieldGetter) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("person_id", fieldGetter.apply(rs, "person_id"));
        document.addField("fnr", fieldGetter.apply(rs, "fodselsnr"));
        document.addField("fornavn", kapitaliser(fieldGetter.apply(rs, "fornavn")));
        document.addField("etternavn", kapitaliser(fieldGetter.apply(rs, "etternavn")));
        document.addField("enhet_id", fieldGetter.apply(rs, "nav_kontor"));
        document.addField("formidlingsgruppekode", fieldGetter.apply(rs, "formidlingsgruppekode"));
        document.addField("iserv_fra_dato", parseDato(fieldGetter.apply(rs, "iserv_fra_dato")));
        document.addField("kvalifiseringsgruppekode", fieldGetter.apply(rs, "kvalifiseringsgruppekode"));
        document.addField("rettighetsgruppekode", fieldGetter.apply(rs, "rettighetsgruppekode"));
        document.addField("hovedmaalkode", fieldGetter.apply(rs, "hovedmaalkode"));
        document.addField("sikkerhetstiltak", fieldGetter.apply(rs, "sikkerhetstiltak_type_kode"));
        document.addField("diskresjonskode", fieldGetter.apply(rs, "fr_kode"));
        document.addField("egen_ansatt", parseJaNei(fieldGetter.apply(rs, "sperret_ansatt"), "sperret_ansatt"));
        document.addField("er_doed", parseJaNei(fieldGetter.apply(rs, "er_doed"), "er_doed"));
        document.addField("doed_fra_dato", parseDato(fieldGetter.apply(rs, "doed_fra_dato")));
        document.addField("veileder_id", fieldGetter.apply(rs, "veilederident"));
        document.addField("fodselsdag_i_mnd", FodselsnummerUtils.lagFodselsdagIMnd(fieldGetter.apply(rs, "fodselsnr")));
        document.addField("fodselsdato", FodselsnummerUtils.lagFodselsdato(fieldGetter.apply(rs, "fodselsnr")));
        document.addField("kjonn", FodselsnummerUtils.lagKjonn(fieldGetter.apply(rs, "fodselsnr")));
        document.addField("ytelse", fieldGetter.apply(rs, "ytelse"));
        document.addField("utlopsdato_mnd_fasett", fieldGetter.apply(rs, "UTLOPSDATOFASETT"));
        document.addField("aap_maxtid_fasett", fieldGetter.apply(rs, "AAPMAXTIDFASETT"));
        document.addField("utlopsdato", parseDato(fieldGetter.apply(rs, "UTLOPSDATO")));
        document.addField("aap_maxtid", parseDato(fieldGetter.apply(rs, "AAPMAXTID")));

        return document;
    }

    static String kapitaliser(String s) {
        return WordUtils.capitalizeFully(s, ' ', '\'', '-');
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
        boolean defaultValue  = false;
        if (janei == null) {
            logger.warn(String.format("%s er ikke satt i databasen, defaulter til %b", name, defaultValue));
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
