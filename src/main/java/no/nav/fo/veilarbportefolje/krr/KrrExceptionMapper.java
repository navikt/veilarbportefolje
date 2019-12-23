package no.nav.fo.veilarbportefolje.krr;

import no.nav.json.JsonUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
class KrrExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response response = exception.getResponse();
        KrrDTO krrDTO = JsonUtils.fromJson(response.getEntity().toString(), KrrDTO.class);
        String message = String.format("Kall feilet med status %s  og melding %s", response.getStatus(), krrDTO.getMelding());
        throw new WebApplicationException(message);
    }
}
