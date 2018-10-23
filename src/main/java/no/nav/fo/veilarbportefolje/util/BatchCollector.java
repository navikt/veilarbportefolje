package no.nav.fo.veilarbportefolje.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

public class BatchCollector<T> implements Collector<T, List<T>, List<T>> {

    private final int batchSize;
    private final Consumer<Collection<T>> batchProcessor;

    public BatchCollector(int batchSize, Consumer<Collection<T>> batchProcessor) {
        this.batchSize = batchSize;
        this.batchProcessor = batchProcessor;
    }

    @Override
    public Supplier<List<T>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<T>, T> accumulator() {
        return (ts, t) -> {
            ts.add(t);
            if (ts.size() >= batchSize) {
                batchProcessor.accept(ts);
                ts.clear();
            }
        };
    }

    @Override
    public BinaryOperator<List<T>> combiner() {
        return (ts, ots) -> {
            batchProcessor.accept(ts);
            batchProcessor.accept(ots);

            return Collections.emptyList();
        };
    }

    @Override
    public Function<List<T>, List<T>> finisher() {
        return (ts) -> {
            if (!ts.isEmpty()) {
                batchProcessor.accept(ts);
            }
            return Collections.emptyList();
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }


}
