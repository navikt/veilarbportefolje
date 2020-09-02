package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        BrukerService.class,
        AktivitetService.class,
        PersistentOppdatering.class
})
public class ServiceConfigTest {
}
