package no.nav.pto.veilarbportefolje.postgres;

import org.springframework.dao.EmptyResultDataAccessException;
import java.util.function.Supplier;

public class PostgresUtils {
    public static <T> T queryForObjectOrNull(Supplier<T> query) {
        try {
            return query.get();
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public static boolean safeBool(Boolean bool) {
        if (bool == null) {
            return false;
        }
        return bool;
    }
}
