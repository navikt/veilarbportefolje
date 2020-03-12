package no.nav.pto.veilarbportefolje.elastic;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.elastic.domene.CountResponse;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.*;
import static no.nav.sbl.util.EnvironmentUtils.resolveHostName;

@Slf4j
public class ElasticUtils {

    public static String createIndexName(String alias) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s", alias, timestamp);
    }

    public static long getCount(String hostname, String username, String password) {
        String url = "http://" + hostname + ":9200/_doc/_count";

        return RestUtils.withClient(client ->
                client
                        .target(url)
                        .request()
                        .header("Authorization", getAuthHeaderValue(username, password))
                        .get(CountResponse.class)
                        .getCount()
        );

    }

    static String getAuthHeaderValue(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    static String getAlias() {
        return String.format("brukerindeks_%s", EnvironmentUtils.requireNamespace());
    }

}
