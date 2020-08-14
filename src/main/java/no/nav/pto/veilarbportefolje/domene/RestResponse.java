package no.nav.pto.veilarbportefolje.domene;

import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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

    public ResponseEntity ok() {
        return createResponse(HttpStatus.OK);
    }

    public ResponseEntity created() {
        return createResponse(HttpStatus.CREATED);
    }

    public ResponseEntity forbidden() {
        return createResponse(HttpStatus.FORBIDDEN);
    }

    public ResponseEntity badRequest() {
        return createResponse(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity notFound() {
        return createResponse(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity createResponse(HttpStatus status) {
        return ResponseEntity.status(status).body(this);
    }
}
