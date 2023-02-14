package no.nav.pto.veilarbportefolje.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Component
@Slf4j
public class ScheduledErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        secureLog.error(throwable.getMessage(), throwable);
    }

}
