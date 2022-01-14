package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpensearchClientConfig {
    String username;
    String password;
    String hostname;
    int port;
    String scheme;
    boolean disableSecurity;
}

