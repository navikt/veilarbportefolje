package no.nav.pto.veilarbportefolje.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    public static String capitalize(String input) {
        StringBuffer stringbf = new StringBuffer();

        Pattern p = Pattern.compile("([a-z\\u00C0-\\u017F\\u0400-\\u04FF'])([a-z\\u00C0-\\u017F\\u0400-\\u04FF]+)");
        Matcher m = p.matcher(input.toLowerCase());
        while (m.find()) {
            m.appendReplacement(
                    stringbf, m.group(1).toUpperCase() + m.group(2).toLowerCase());
        }

        return m.appendTail(stringbf).toString();
    }
}
