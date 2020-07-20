package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.metrikker.MetricsUtils;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingStatus.fromJson;

@Service
@Slf4j
public class OppfolgingService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elastic;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final NavKontorService navKontorService;
    private final ArbeidslisteService arbeidslisteService;
    private final UnleashService unleashService;
    private final AktorregisterClient aktorregisterClient;
    private final CvService cvService;
    private final MetricsClient metricsClient;

    @Autowired
    public OppfolgingService(OppfolgingRepository oppfolgingRepository,
                             ElasticIndexer elastic,
                             VeilarbVeilederClient veilarbVeilederClient,
                             NavKontorService navKontorService,
                             ArbeidslisteService arbeidslisteService,
                             UnleashService unleashService,
                             AktorregisterClient aktorregisterClient,
                             CvService cvService,
                             MetricsClient metricsClient
    ) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.elastic = elastic;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.navKontorService = navKontorService;
        this.arbeidslisteService = arbeidslisteService;
        this.unleashService = unleashService;
        this.aktorregisterClient = aktorregisterClient;
        this.cvService = cvService;
        this.metricsClient = metricsClient;
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

        if (brukerenIkkeLengerErUnderOppfolging(oppfolgingStatus)) {
            Result<Integer> result = cvService.setHarDeltCvTilNei(aktoerId);
            if (result.err().isPresent()) {
                log.error("Kunne ikke sette har delt cv til nei for bruker " + aktoerId, result.err().get());
            }
        }

        Optional<VeilederId> eksisterendeVeileder = hentEksisterendeVeileder(aktoerId);
        Optional<VeilederId> nyVeileder = oppfolgingStatus.getVeilederId();
        if (
                brukerenIkkeLengerErUnderOppfolging(oppfolgingStatus) ||
                eksisterendeVeilederHarIkkeTilgangTilBrukerensEnhet(aktoerId, nyVeileder, eksisterendeVeileder)
        ) {
            slettArbeidsliste(aktoerId);
        }


        MetricsUtils.timed(
                "portefolje.oppfolging.oppdater",
                () -> oppfolgingRepository.oppdaterOppfolgingData(oppfolgingStatus).orElseThrowException(),
                metricsClient
        );

        MetricsUtils.timed(
                "portefolje.oppfolging.indekser",
                () -> elastic.indekser(aktoerId).orElseThrowException(),
                metricsClient
        );
    }

    boolean eksisterendeVeilederHarIkkeTilgangTilBrukerensEnhet(AktoerId aktoerId, Optional<VeilederId> nyVeileder, Optional<VeilederId> eksisterendeVeileder) {
        return nyVeileder.isPresent()
               && eksisterendeVeileder.isPresent()
               && !veilederHarTilgangTilBrukerensEnhet(eksisterendeVeileder.get(), aktoerId);
    }

    private void slettArbeidsliste(AktoerId aktoerId) {
        log.info("Sletter arbeidsliste for bruker {}", aktoerId);
        Result<Integer> result = arbeidslisteService.deleteArbeidslisteForAktoerId(aktoerId);
        if (result.isErr()) {
            log.error("Kunne ikke slette arbeidsliste for bruker {}", aktoerId);
        }
    }

    boolean veilederHarTilgangTilBrukerensEnhet(VeilederId veilederId, AktoerId aktoerId) {

        Fnr fnr = MetricsUtils.timed(
                "portefolje.oppfolging.hentFnr",
                () -> Fnr.of(aktorregisterClient.hentFnr(aktoerId.toString())),
                metricsClient
        );

        String enhet = MetricsUtils.timed(
                "portefolje.oppfolging.hentEnhet",
                () -> navKontorService.hentEnhetForBruker(fnr).orElseThrowException(),
                metricsClient
        );

        List<VeilederId> veilederePaaEnhet = MetricsUtils.timed(
                "portefolje.oppfolging.hentVeileder",
                () -> veilarbVeilederClient.hentVeilederePaaEnhet(enhet),
                metricsClient
        );

        return veilederePaaEnhet.contains(veilederId);
    }

    Optional<VeilederId> hentEksisterendeVeileder(AktoerId aktoerId) {
        return MetricsUtils.timed(
                "portefolje.oppfolging.hentVeileder",
                () -> oppfolgingRepository.hentOppfolgingData(aktoerId).ok()
                        .map(info -> info.getVeileder())
                        .map(VeilederId::new),
                metricsClient
        );
    }

    static boolean brukerenIkkeLengerErUnderOppfolging(OppfolgingStatus oppfolgingStatus) {
        return !oppfolgingStatus.isOppfolging();
    }
}
