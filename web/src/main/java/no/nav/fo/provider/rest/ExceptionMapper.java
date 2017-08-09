package no.nav.fo.provider.rest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    @Inject
    javax.inject.Provider<HttpServletRequest> servletRequestProvider;

    @Override
    public Response toResponse(Throwable exception) {
        return new no.nav.apiapp.rest.ExceptionMapper(servletRequestProvider).toResponse(exception);
    }

}
