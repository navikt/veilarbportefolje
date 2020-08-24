package no.nav.pto.veilarbportefolje.feed.consumer;

import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;

import java.util.List;

@FunctionalInterface
public interface FeedCallback {
    void call(String lastEntryId, List<BrukerOppdatertInformasjon> data);
}
