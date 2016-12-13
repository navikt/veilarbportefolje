package no.nav.sbl.fo.provider.rest.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/logging")
public class JSLoggerController {
    Logger logger = LoggerFactory.getLogger("frontendlog");

    @POST
    @Consumes(APPLICATION_JSON)
    public void log(LogLinje logLinje) {
        switch (logLinje.level) {
            case "INFO":
                logger.info(logLinje.toString());
                break;
            case "WARN":
                logger.warn(logLinje.toString());
                break;
            case "ERROR":
                logger.error(logLinje.toString());
                break;
            default:
                logger.error("Level-field for LogLinje ikke godkjent.", logLinje);
        }
    }
}
