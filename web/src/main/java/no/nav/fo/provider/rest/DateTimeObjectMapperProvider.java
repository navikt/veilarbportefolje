package no.nav.fo.provider.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import static no.nav.apiapp.rest.DateConfiguration.dateModule;

@Provider
public class DateTimeObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DateTimeObjectMapperProvider.class);

    public DateTimeObjectMapperProvider() {
        objectMapper = createObjectMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(dateModule());
    }
}
