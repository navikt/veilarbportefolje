package no.nav.pto.veilarbportefolje.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class FnrUsageLoggerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fnrPattern = ".*\\b\\d{11}\\b.*";

        boolean hasFnrInRequestURI = StringUtils.notNullOrEmpty(requestURI) && requestURI.matches(fnrPattern);
        boolean hasFnrInQueryString = StringUtils.notNullOrEmpty(queryString) && queryString.matches(fnrPattern);

        if (hasFnrInRequestURI || hasFnrInQueryString) {
            log.info("Konsument forespurte endepunkt som matcher fnr-regex.");
        }

        return true;
    }
}
