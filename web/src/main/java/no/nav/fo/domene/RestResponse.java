package no.nav.fo.domene;

import lombok.Value;

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

    public static <T> RestResponse<T> merge(RestResponse<T> response1, RestResponse<T> response2) {
        ArrayList<String> errorsCopy = new ArrayList<>(response1.getError());
        errorsCopy.addAll(response2.getError());
        ArrayList<T> dataCopy = new ArrayList<>(response1.getData());
        dataCopy.addAll(response2.getData());
        return new RestResponse<>(errorsCopy, dataCopy);
    }

}
