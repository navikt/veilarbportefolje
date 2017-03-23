package no.nav.fo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collector;

public class StreamUtils {
    static Logger logger = LoggerFactory.getLogger(StreamUtils.class);

    public static <T> Collector<T, List<T>, List<T>> batchCollector(int batchSize, Consumer<Collection<T>> batchProcessor) {
        return new BatchCollector<>(batchSize, batchProcessor);
    }

    public static <T> List<T> batchProcess(int batchSize, Collection<T> list, Consumer<Collection<T>> batchProcessor) {
        return list.stream().collect(batchCollector(batchSize, batchProcessor));
    }

    public static Consumer<Throwable> log(String melding) {
        return (throwable) -> logger.error(melding, throwable);
    }

    public static Consumer<Throwable> log(Logger logger, String melding) {
        return (throwable) -> logger.error(melding, throwable);
    }
}
