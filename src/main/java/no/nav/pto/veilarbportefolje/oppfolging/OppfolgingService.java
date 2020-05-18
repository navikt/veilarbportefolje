package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.service.NavKontorService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingStatus.fromJson;

@Slf4j
public class OppfolgingService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elastic;
    private final VeilederService veilederService;
    private final NavKontorService navKontorService;
    private final ArbeidslisteService arbeidslisteService;
    private final UnleashService unleashService;
    private final AktoerService aktoerService;

    public OppfolgingService(OppfolgingRepository oppfolgingRepository,
                             ElasticIndexer elastic,
                             VeilederService veilederService,
                             NavKontorService navKontorService,
                             ArbeidslisteService arbeidslisteService,
                             UnleashService unleashService,
                             AktoerService aktoerService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.elastic = elastic;
        this.veilederService = veilederService;
        this.navKontorService = navKontorService;
        this.arbeidslisteService = arbeidslisteService;
        this.unleashService = unleashService;
        this.aktoerService = aktoerService;
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

    boolean eksisterendeVeilederHarTilgangTilBruker(AktoerId aktoerId) {
        Optional<VeilederId> eksisterendeVeileder = oppfolgingRepository.hentOppfolgingData(aktoerId).ok()
                .map(info -> info.getVeileder())
                .map(VeilederId::new);

        if (!eksisterendeVeileder.isPresent()) {
            return false;
        }

        Fnr fnr = aktoerService.hentFnrFraAktorId(aktoerId).getOrElseThrow(() -> new IllegalStateException());

        Result<List<VeilederId>> result = navKontorService.hentEnhetForBruker(fnr)
                .mapOk(enhet -> veilederService.hentVeilederePaaEnhet(enhet));

        return eksisterendeVeileder
                .flatMap(veileder -> result.ok().map(veilederePaaEnhet -> veilederePaaEnhet.contains(veileder)))
                .orElse(false);
    }

    static boolean brukerenIkkeLengerErUnderOppfolging(OppfolgingStatus oppfolgingStatus) {
        return !oppfolgingStatus.isOppfolging();
    }
}
