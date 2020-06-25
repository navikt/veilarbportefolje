package no.nav.pto.veilarbportefolje.feed.common;

import javax.ws.rs.client.Invocation;

public interface OutInterceptor {
    void apply(Invocation.Builder builder);
}
