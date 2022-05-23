package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.IndexName;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    protected OpensearchAdminService opensearchAdminService;

    @Autowired
    protected OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @BeforeEach
    void setUp() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(Optional.ofNullable(System.getenv("TZ")).orElse("Europe/Oslo")));
            opensearchAdminService.opprettNyIndeks(indexName.getValue());
        } catch (Exception e) {
            opensearchAdminService.slettIndex(indexName.getValue());
            opensearchAdminService.opprettNyIndeks(indexName.getValue());
        }
    }

    @AfterEach
    void tearDown() {
        opensearchAdminService.slettIndex(indexName.getValue());
    }

    public void populateOpensearch(NavKontor enhetId, VeilederId veilederId, String... aktoerIder) {
        List<OppfolgingsBruker> brukere = new ArrayList<>();
        for (String aktoerId : aktoerIder) {
            oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(aktoerId), ZonedDateTime.now());
            brukere.add(new OppfolgingsBruker()
                    .setAktoer_id(aktoerId)
                    .setOppfolging(true)
                    .setEnhet_id(enhetId.getValue())
                    .setVeileder_id(veilederId.getValue())
            );
        }
        brukere.forEach(bruker -> opensearchTestClient.createUserInOpensearch(bruker));
    }

    @SneakyThrows
    public static void verifiserAsynkront(long timeout, TimeUnit unit, Runnable verifiser) {
        long timeoutMillis = unit.toMillis(timeout);
        boolean prosessert = false;
        boolean timedOut = false;
        long start = System.currentTimeMillis();
        while (!prosessert) {
            try {
                Thread.sleep(10);
                long current = System.currentTimeMillis();
                timedOut = current - start > timeoutMillis;
                verifiser.run();
                prosessert = true;
            } catch (Throwable a) {
                if (timedOut) {
                    throw a;
                }
            }
        }
    }
}
