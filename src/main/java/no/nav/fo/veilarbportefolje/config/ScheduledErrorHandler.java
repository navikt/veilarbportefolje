package no.nav.fo.veilarbportefolje.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

@Component
@Slf4j
public class ScheduledErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
    }

}
