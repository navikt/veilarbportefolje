package no.nav.pto.veilarbportefolje.elastic;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAuthHeaderValue;

@Provider
public class ElasticAuthFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        ctx.getHeaders().add("Authorization", getAuthHeaderValue(VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME, VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD));
    }
}
