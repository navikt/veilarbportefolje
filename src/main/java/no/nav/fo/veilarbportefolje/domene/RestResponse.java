package no.nav.fo.veilarbportefolje.domene;

import lombok.Value;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.*;

@Value
public class RestResponse<T> {
    public List<String> error;
    public List<T> data;

    public static <T> RestResponse<T> of(T data) {
        return new RestResponse<>(emptyList(), singletonList(data));
    }

    public static <T> RestResponse<T> of(String error) {

        return new RestResponse<>(singletonList(error), emptyList());
    }
    public static <T> RestResponse<T> of(List<String> error) {
        return new RestResponse<>(error, emptyList());
    }

    public static <T> RestResponse<T> of(List<T> data, List<String> error) {
        return new RestResponse<>(error, data);
    }

    public static <T> RestResponse<T> merge(RestResponse<T> response1, RestResponse<T> response2) {
        ArrayList<String> errorsCopy = new ArrayList<>(response1.getError());
        errorsCopy.addAll(response2.getError());
        ArrayList<T> dataCopy = new ArrayList<>(response1.getData());
        dataCopy.addAll(response2.getData());
        return new RestResponse<>(errorsCopy, dataCopy);
    }

    public void addData(T data) {
        this.data.add(data);
    }

    public void addError(String error) {
        this.error.add(error);
    }

    public boolean containsError() { return !error.isEmpty(); }

    public Response ok() {
        return createResponse(OK);
    }

    public Response created() {
        return createResponse(CREATED);
    }

    public Response forbidden() {
        return createResponse(FORBIDDEN);
    }

    public Response badRequest() {
        return createResponse(BAD_REQUEST);
    }

    public Response notFound() {
        return createResponse(NOT_FOUND);
    }

    private Response createResponse(Response.Status status) {
        return Response.status(status).entity(this).build();
    }
}
