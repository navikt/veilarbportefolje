package no.nav.pto.veilarbportefolje.util;

import java.util.Arrays;

public class EnumUtils {

    private EnumUtils() {
    }

    public static String getName(Enum<?> anEnum) {
        return anEnum != null ? anEnum.name() : null;
    }

    public static <T extends Enum<T>> T valueOfOrNull(Class<T> enumClass, String name) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny()
                .orElse(null);
    }
}
