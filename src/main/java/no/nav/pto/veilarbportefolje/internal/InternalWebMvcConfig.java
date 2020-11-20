package no.nav.pto.veilarbportefolje.internal;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/internal/admin").setViewName("redirect:/internal/admin/");
        registry.addViewController("/internal/admin/").setViewName("forward:/internal/admin/index.html");
    }
}
