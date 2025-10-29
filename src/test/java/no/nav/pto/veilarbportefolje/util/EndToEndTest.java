package no.nav.pto.veilarbportefolje.util;

import io.getunleash.DefaultUnleash;
import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.IndexName;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


@SpringBootTest(classes = ApplicationConfigTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class EndToEndTest {

    @Autowired
    protected OpensearchTestClient opensearchTestClient;

    @Autowired
    protected TestDataClient testDataClient;

    @Autowired
    protected OpensearchIndexer opensearchIndexer;

    @Autowired
    protected PdlIdentRepository pdlIdentRepository;

    @Autowired
    protected IndexName indexName;

    @Autowired
    protected DefaultUnleash defaultUnleash;

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
        List<PortefoljebrukerOpensearchModell> brukere = new ArrayList<>();
        for (String aktoerId : aktoerIder) {

            PortefoljebrukerOpensearchModell bruker = new PortefoljebrukerOpensearchModell();
            bruker.setAktoer_id(aktoerId);
            bruker.setOppfolging(true);
            bruker.setEnhet_id(enhetId.getValue());
            bruker.setVeileder_id(veilederId.getValue());

            brukere.add(bruker);
        }
        brukere.forEach(bruker -> opensearchTestClient.createUserInOpensearch(bruker));
    }

    public void insertOppfolgingsInformasjon(AktorId aktorId, Fnr fnr) {
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
    }

    public void slettOppfolgingsInformasjon(AktorId aktorId) {
        oppfolgingRepositoryV2.slettOppfolgingData(aktorId);
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
