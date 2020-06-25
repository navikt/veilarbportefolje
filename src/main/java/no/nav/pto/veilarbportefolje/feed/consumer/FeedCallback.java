package no.nav.pto.veilarbportefolje.feed.consumer;

import java.util.List;

@FunctionalInterface
public interface FeedCallback<DOMAINOBJECT> {
    void call(String lastEntryId, List<DOMAINOBJECT> data);
}
