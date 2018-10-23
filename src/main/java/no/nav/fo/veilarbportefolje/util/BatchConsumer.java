package no.nav.fo.veilarbportefolje.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BatchConsumer<T> implements Consumer<T> {
    final List<T> batch;
    final int size;
    final Consumer<List<T>> consumer;

    public BatchConsumer(int size, Consumer<List<T>> consumer) {
        this.size = size;
        this.batch = new ArrayList<>(size);
        this.consumer = consumer;
    }

    public static <S> BatchConsumer<S> batchConsumer(int size, Consumer<List<S>> consumer) {
        return new BatchConsumer<>(size, consumer);
    }

    public void flush() {
        consumer.accept(batch);
        batch.clear();
    }

    @Override
    public void accept(T t) {
        batch.add(t);
        if (batch.size() == size) {
            this.flush();
        }
    }
}
