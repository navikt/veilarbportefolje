package no.nav.pto.veilarbportefolje.feed.common;

@FunctionalInterface
public interface FeedAuthorizationModule {
    boolean isRequestAuthorized(String feedname);
}
