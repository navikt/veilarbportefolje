package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.IndexName;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@SpringBootTest(classes = ApplicationConfigTest.class)
public abstract class EndToEndTest {

    @Autowired
    protected ElasticTestClient elasticTestClient;

    @Autowired
    protected TestDataClient testDataClient;

    @Autowired
    protected ElasticIndexer elasticIndexer;

    @Autowired
    protected IndexName indexName;

    @Autowired
    protected UnleashService unleashService;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @BeforeEach
    void setUp() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(Optional.ofNullable(System.getenv("TZ")).orElse("Europe/Oslo")));
            ElasticTestClient.opprettNyIndeks(indexName.getValue(), restHighLevelClient);
        } catch (Exception e) {
            elasticTestClient.deleteIndex(indexName);
            ElasticTestClient.opprettNyIndeks(indexName.getValue(), restHighLevelClient);
        }
    }

    @AfterEach
    void tearDown() {
        elasticTestClient.deleteIndex(indexName);
    }

    public void populateElastic(EnhetId enhetId, VeilederId veilederId, String... aktoerIder) {
        List<OppfolgingsBruker> brukere = new ArrayList<>();
        for (String aktoerId : aktoerIder) {
            brukere.add(new OppfolgingsBruker()
                    .setAktoer_id(aktoerId)
                    .setOppfolging(true)
                    .setEnhet_id(enhetId.get())
                    .setVeileder_id(veilederId.getValue())
            );
        }

        brukere.forEach(bruker -> elasticTestClient.createUserInElastic(bruker));
    }
}
