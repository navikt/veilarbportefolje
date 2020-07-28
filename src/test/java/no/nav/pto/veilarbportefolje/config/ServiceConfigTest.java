package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.pto.veilarbportefolje.service.PersonIdService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        PersonIdService.class,
        AktivitetService.class,
        PersistentOppdatering.class
})
public class ServiceConfigTest {
}
