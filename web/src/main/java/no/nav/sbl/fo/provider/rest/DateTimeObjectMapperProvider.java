package no.nav.sbl.fo.provider.rest;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

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
        logger.debug("Logger ObjectCoder pga prosjektet ikke kompilerer når jackson-core har scope runtime, og dependency-checker'n klager når den har scope compile",
                ObjectCodec.class);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
