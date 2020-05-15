package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.NavKontorService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingStatus.fromJson;

@Slf4j
public class OppfolgingService implements KafkaConsumerService {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elastic;
    private final VeilederService veilederService;
    private final NavKontorService navKontorService;
    private final ArbeidslisteService arbeidslisteService;
    private final UnleashService unleashService;
    private final ExecutorService threadPool;

    public OppfolgingService(OppfolgingRepository oppfolgingRepository, ElasticIndexer elastic, VeilederService veilederService, NavKontorService navKontorService, ArbeidslisteService arbeidslisteService, UnleashService unleashService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.elastic = elastic;
        this.veilederService = veilederService;
        this.navKontorService = navKontorService;
        this.arbeidslisteService = arbeidslisteService;
        this.unleashService = unleashService;
        this.threadPool = Executors.newFixedThreadPool(3);
    }

    @Override
    @Transactional
    public void behandleKafkaMelding(String kafkaMelding) {
        if (!unleashService.isEnabled(KafkaConfig.KAFKA_OPPFOLGING_BEHANDLE_MELDINGER_TOGGLE)) {
            log.info("Ingorerer melding fra kafka");
            return;
        }

        OppfolgingStatus oppfolgingStatus = fromJson(kafkaMelding);
        AktoerId aktoerId = oppfolgingStatus.getAktoerId();

        if (oppfolgingStatus.getStartDato() == null) {
            log.warn("Bruker {} har ikke startDato", aktoerId);
        }

        if (brukerenIkkeLengerErUnderOppfolging(oppfolgingStatus) || eksisterendeVeilederHarIkkeTilgangTilBruker(aktoerId)) {
            Result<Integer> result = arbeidslisteService.deleteArbeidslisteForAktoerId(aktoerId);
            if (result.isErr()) {
                log.error("Kunne ikke slette arbeidsliste for bruker {}", aktoerId);
            }
        }

        oppfolgingRepository.oppdaterOppfolgingData(oppfolgingStatus)
                .orElseThrowException();

        elastic.indekser(aktoerId)
                .orElseThrowException();
    }

    boolean eksisterendeVeilederHarIkkeTilgangTilBruker(AktoerId aktoerId) {
        return !eksisterendeVeilederHarTilgangTilBruker(aktoerId);
    }

    @SneakyThrows
    boolean eksisterendeVeilederHarTilgangTilBruker(AktoerId aktoerId) {

        CompletableFuture<Result<BrukerOppdatertInformasjon>> future = hentBrukerInfoAsync(aktoerId);
        List<VeilederId> veilederePaaEnhet = hentVeilederePaaEnhet(aktoerId);

        Optional<VeilederId> eksisterendeVeileder = future.get()
                .ok()
                .map(BrukerOppdatertInformasjon::getVeileder)
                .map(VeilederId::of);

        return eksisterendeVeileder.isPresent() && veilederePaaEnhet.contains(eksisterendeVeileder.get());
    }

    private List<VeilederId> hentVeilederePaaEnhet(AktoerId aktoerId) {
        return navKontorService.hentEnhetForBruker(aktoerId)
                .mapOk(enhet -> veilederService.hentVeilederePaaEnhet(enhet))
                .orElse(emptyList());
    }

    private CompletableFuture<Result<BrukerOppdatertInformasjon>> hentBrukerInfoAsync(AktoerId aktoerId) {
        Supplier<Result<BrukerOppdatertInformasjon>> supplier = () -> oppfolgingRepository.hentOppfolgingData(aktoerId);
        return CompletableFuture.supplyAsync(supplier, this.threadPool);
    }

    static boolean brukerenIkkeLengerErUnderOppfolging(OppfolgingStatus oppfolgingStatus) {
        return !oppfolgingStatus.isOppfolging();
    }
}
