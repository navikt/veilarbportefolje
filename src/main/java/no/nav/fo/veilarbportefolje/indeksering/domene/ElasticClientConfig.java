package no.nav.fo.veilarbportefolje.indeksering.domene;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ElasticClientConfig {
    String username;
    String password;
    String hostname;
    int port;
    String scheme;
}

