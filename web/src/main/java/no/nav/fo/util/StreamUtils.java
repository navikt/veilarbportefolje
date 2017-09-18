package no.nav.fo.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collector;

@Slf4j
public class StreamUtils {

    public static <T> Collector<T, List<T>, List<T>> batchCollector(int batchSize, Consumer<Collection<T>> batchProcessor) {
        return new BatchCollector<>(batchSize, batchProcessor);
    }

    public static <T> List<T> batchProcess(int batchSize, Collection<T> list, Consumer<Collection<T>> batchProcessor) {
        return list.stream().collect(batchCollector(batchSize, batchProcessor));
    }

    public static Consumer<Throwable> log(String melding) {
        return (throwable) -> log.error(melding, throwable);
    }

    public static Consumer<Throwable> log(Logger logger, String melding) {
        return (throwable) -> logger.error(melding, throwable);
    }
}
