package no.nav.pto.veilarbportefolje.feed.util;

import no.nav.common.utils.EnvironmentUtils;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class UrlUtils {

    public final static String QUERY_PARAM_PAGE_SIZE = "page_size";
    public final static String QUERY_PARAM_ID = "id";

    private static Pattern pattern = Pattern.compile("([^:]\\/)\\/+");

    public static String callbackUrl(String root, String feedname) {
        return asUrl(getHost(), root, "feed", feedname);
    }

    private static String getHost() {
        if ("dev-fss".equals(EnvironmentUtils.getClusterName().orElse("dev-fss"))) {
            return format("https://app-%s.adeo.no", EnvironmentUtils.getNamespace());
        }
        return "https://app.adeo.no";
    }

    public static String asUrl(String... s) {
        String url = Stream.of(s).collect(Collectors.joining("/"));
        return pattern.matcher(url).replaceAll("$1");
    }
}
