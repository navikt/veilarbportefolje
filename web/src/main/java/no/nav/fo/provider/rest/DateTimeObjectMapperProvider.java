package no.nav.fo.provider.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.jackson.datatype.VavrModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import static no.nav.fo.provider.rest.DateConfiguration.dateModule;

@Provider
public class DateTimeObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private ObjectMapper objectMapper;

    public DateTimeObjectMapperProvider() {
        objectMapper = createObjectMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(dateModule())
                .registerModule(new VavrModule());
    }
}
