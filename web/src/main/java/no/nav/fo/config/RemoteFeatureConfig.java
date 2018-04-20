package no.nav.fo.config;
import no.nav.sbl.featuretoggle.remote.RemoteFeatureToggle;
import no.nav.sbl.featuretoggle.remote.RemoteFeatureToggleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class RemoteFeatureConfig {

    @Value("${feature_endpoint.url}")
    private String remoteFeatureUrl;

    @Bean
    public RemoteFeatureToggleRepository remoteFeatureToggleRespository() {
        return new RemoteFeatureToggleRepository(remoteFeatureUrl);
    }

    @Bean
    public FlyttSomNyeFeature FlyttSomNyeFeature(RemoteFeatureToggleRepository repository) {
        return new FlyttSomNyeFeature(repository);
    }

    public static class FlyttSomNyeFeature extends RemoteFeatureToggle {
        public FlyttSomNyeFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarbportefolje.flyttedebrukere");
        }

    }
}
