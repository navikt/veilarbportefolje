package no.nav.fo.veilarbportefolje.config;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.KrrRepository;
import no.nav.fo.veilarbportefolje.database.PersistentOppdatering;
import no.nav.fo.veilarbportefolje.service.*;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;

@Configuration
public class ServiceConfig {

    @Bean
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public ArbeidslisteService arbeidslisteService() {
        return new ArbeidslisteService();
    }

    @Bean
    public AktivitetService aktivitetService(AktoerService aktoerService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering) {
        return new AktivitetService(aktoerService, aktivitetDAO, persistentOppdatering);
    }

    @Bean
    public TiltakService tiltakService() {
        return new TiltakService();
    }

    @Bean
    public KrrService krrService(KrrRepository krrRepository, DigitalKontaktinformasjonV1 dkif, LockingTaskExecutor lockingTaskExecutor) {
        return new KrrService(krrRepository, dkif, lockingTaskExecutor);
    }

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName(requireApplicationName())
                .unleashApiUrl(getRequiredProperty(UNLEASH_API_URL_PROPERTY_NAME))
                .build());
    }

    @Bean
    public VeilederService veilederservice(Client restClient) {
        return new VeilederService(restClient);
    }

}
