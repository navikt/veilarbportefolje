package no.nav.fo.config.feed;

import no.nav.sbl.dialogarena.types.Pingable;

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
        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes(name);
                }
            } catch (IOException e) {
                return Pingable.Ping.feilet(name, e);
            }
            return Pingable.Ping.feilet(name, new RuntimeException("Noe gikk feil."));
        };
    }
}
