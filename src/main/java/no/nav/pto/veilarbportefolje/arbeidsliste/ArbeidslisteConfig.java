package no.nav.pto.veilarbportefolje.arbeidsliste;

import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ArbeidslisteConfig {

    @Bean
    @Primary
    @Autowired
    ArbeidslisteService arbeidslisteServiceOracle(AktorClient aktorClient, ArbeidslisteRepositoryV1 arbeidslisteRepository,
                                                  BrukerService brukerService, ElasticServiceV2 elasticServiceV2,
                                                  MetricsClient metricsClient) {
        return new ArbeidslisteService(aktorClient, arbeidslisteRepository, brukerService, elasticServiceV2, metricsClient);
    }

    @Bean("PostgresArbeidslisteService")
    @Autowired
    ArbeidslisteService arbeidslisteService(AktorClient aktorClient, ArbeidslisteRepositoryV2 arbeidslisteRepository,
                                            BrukerService brukerService,
                                            ElasticServiceV2 elasticServiceV2, MetricsClient metricsClient) {
        return new ArbeidslisteService(aktorClient, arbeidslisteRepository, brukerService, elasticServiceV2, metricsClient);
    }
}
