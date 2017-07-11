package no.nav.fo.domene;

import io.vavr.collection.List;
import lombok.Value;

@Value
public class RestResponse<T> {
    List<String> errors;
    List<T> data;

    public static <T> RestResponse<T> of(T data) {
        return new RestResponse<>(List.empty(), List.of(data));
    }

    public static <T> RestResponse<T> merge(RestResponse<T> response1, RestResponse<T> response2) {
        List<String> errors = response1.getErrors().pushAll(response2.getErrors());
        List<T> data = response1.getData().pushAll(response2.getData());
        return new RestResponse<>(errors, data);
    }

}
