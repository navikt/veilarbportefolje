package no.nav.fo.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListUtils {

    @SafeVarargs
    public static <T,S> List<T> distinctByPropertyList(Function<T,S> distinctBy, List<T> ...lists) {
        return new ArrayList<>(Stream.of(lists)
                .map(Collection::stream)
                .reduce(Stream::concat)
                .orElse(Stream.empty())
                .collect(Collectors.toMap(distinctBy, t->t, (p, q)->p))
                .values()
        );
    }
}
