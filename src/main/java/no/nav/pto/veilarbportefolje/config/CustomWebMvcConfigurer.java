package no.nav.pto.veilarbportefolje.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CustomWebMvcConfigurer implements WebMvcConfigurer {

    private final FnrUsageLoggerInterceptor fnrUsageLoggerInterceptor;

    @Autowired
    public CustomWebMvcConfigurer(final FnrUsageLoggerInterceptor fnrUsageLoggerInterceptor) {
        this.fnrUsageLoggerInterceptor = fnrUsageLoggerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = List.of("/internal", "/internal/**");

        registry.addInterceptor(fnrUsageLoggerInterceptor).excludePathPatterns(excludePaths);
    }
}
