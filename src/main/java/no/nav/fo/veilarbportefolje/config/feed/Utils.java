package no.nav.fo.veilarbportefolje.config.feed;

import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Function;
import java.util.function.Supplier;

public class Utils {

    public static <S, T> Supplier<T> apply(Function<S, T> fn, S arg) {
        return () -> fn.apply(arg);
    }

    public static Pingable urlPing(String name, String url) {
        PingMetadata metadata = new PingMetadata(url, name, true);

        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes(metadata);
                }
            } catch (IOException e) {
                return Pingable.Ping.feilet(metadata, e);
            }
            return Pingable.Ping.feilet(metadata, new RuntimeException("URL-Ping feilet."));
        };
    }
}
