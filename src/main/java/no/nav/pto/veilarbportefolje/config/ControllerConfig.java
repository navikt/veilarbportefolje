package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.api.EnhetController;
import no.nav.pto.veilarbportefolje.api.PersoninfoController;
import no.nav.pto.veilarbportefolje.api.VeilederController;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidsListeController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        EnhetController.class,
        PersoninfoController.class,
        VeilederController.class,
        ArbeidsListeController.class,
})
public class ControllerConfig {
}
