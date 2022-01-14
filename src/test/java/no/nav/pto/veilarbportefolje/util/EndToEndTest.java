package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.opensearch.IndexName;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
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
    protected OpensearchTestClient opensearchTestClient;

    @Autowired
    protected TestDataClient testDataClient;

    @Autowired
    protected OpensearchIndexer opensearchIndexer;

    @Autowired
    protected OpensearchIndexerV2 opensearchIndexerV2;

    @Autowired
    protected IndexName indexName;

    @Autowired
    protected UnleashService unleashService;

    @BeforeEach
    void setUp() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(Optional.ofNullable(System.getenv("TZ")).orElse("Europe/Oslo")));
            opensearchIndexerV2.opprettNyIndeks(indexName.getValue());
        } catch (Exception e) {
            opensearchIndexerV2.slettIndex(indexName.getValue());
            opensearchIndexerV2.opprettNyIndeks(indexName.getValue());
        }
    }

    @AfterEach
    void tearDown() {
        opensearchIndexerV2.slettIndex(indexName.getValue());
    }

    public void populateOpensearch(EnhetId enhetId, VeilederId veilederId, String... aktoerIder) {
        List<OppfolgingsBruker> brukere = new ArrayList<>();
        for (String aktoerId : aktoerIder) {
            brukere.add(new OppfolgingsBruker()
                    .setAktoer_id(aktoerId)
                    .setOppfolging(true)
                    .setEnhet_id(enhetId.get())
                    .setVeileder_id(veilederId.getValue())
            );
        }

        brukere.forEach(bruker -> opensearchTestClient.createUserInOpensearch(bruker));
    }
}
