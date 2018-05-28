package no.nav.fo.consumer;

import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;
import no.nav.fo.feed.consumer.FeedCallback;

import java.util.List;
import java.util.stream.Collectors;

public class DedupeFeedHandler<TYPE> implements FeedCallback<TYPE> {
    private final FeedCallback<TYPE> handler;
    private final int memorySize;
    private Set<TYPE> cache;

    public static <TYPE> DedupeFeedHandler<TYPE> of(int memorySize, FeedCallback<TYPE> handler) {
        return new DedupeFeedHandler<>(handler, memorySize);
    }


    private DedupeFeedHandler(FeedCallback<TYPE> handler, int memorySize) {
        this.handler = handler;
        this.memorySize = memorySize;
        this.cache = LinkedHashSet.empty();
    }

    @Override
    public void call(String s, List<TYPE> list) {
        List<TYPE> notReportedBefore = list
                .stream()
                .filter(this::cacheContains)
                .collect(Collectors.toList());

        if (notReportedBefore.isEmpty()) {
            return;
        }

        int retainCount = this.memorySize - list.size();
        Set<TYPE> previousReported = this.cache.removeAll(list);
        this.cache = LinkedHashSet.ofAll(list)
                .addAll(previousReported.take(retainCount))
                .take(this.memorySize);

        this.handler.call(s, notReportedBefore);
    }

    int size() {
        return this.cache.size();
    }

    List<TYPE> getList() {
        return this.cache.toJavaList();
    }

    private boolean cacheContains(TYPE type) {
        return !this.cache.contains(type);
    }
}
