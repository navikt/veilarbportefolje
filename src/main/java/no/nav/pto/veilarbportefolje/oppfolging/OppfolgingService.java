package no.nav.pto.veilarbportefolje.oppfolging;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.NavKontorService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.sbl.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

import static no.nav.json.JsonUtils.fromJson;


@Slf4j
public class OppfolgingService implements KafkaConsumerService {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elastic;
    private final VeilederService veilederService;
    private final NavKontorService navKontorService;
    private final ArbeidslisteService arbeidslisteService;

    public final static Supplier<IllegalStateException> ILLEGAL_STATE_EXCEPTION = () -> new IllegalStateException();

    public OppfolgingService(OppfolgingRepository oppfolgingRepository, ElasticIndexer elastic, VeilederService veilederService, NavKontorService navKontorService, ArbeidslisteService arbeidslisteService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.elastic = elastic;
        this.veilederService = veilederService;
        this.navKontorService = navKontorService;
        this.arbeidslisteService = arbeidslisteService;
    }

    @Override
    @Transactional
    public void behandleKafkaMelding(String kafkaMelding) {
        val dto = fromJson(kafkaMelding, OppfolgingDTO.class);
        val startDato = dto.getStartDato();
        val aktoerId = dto.getAktoerId();

        if (startDato == null) {
            log.warn("Bruker {} har ikke startDato", aktoerId);
        }

        if (brukerenIkkeLengerErUnderOppfolging(dto) || eksisterendeVeilederIkkeLengerHarTilgangTilBruker(aktoerId)) {
            arbeidslisteService.deleteArbeidslisteForAktoerId(aktoerId);
        }
        oppfolgingRepository.oppdaterOppfolgingData(dto);
    }

    boolean brukerenHarEnVeileder(AktoerId aktoerId) {
        Try<BrukerOppdatertInformasjon> result = oppfolgingRepository.retrieveOppfolgingData(aktoerId);
        return result
                .map(BrukerOppdatertInformasjon::getVeileder)
                .map(StringUtils::notNullOrEmpty)
                .getOrElse(false);
    }

    boolean eksisterendeVeilederIkkeLengerHarTilgangTilBruker(AktoerId aktoerId) {
        return brukerenHarEnVeileder(aktoerId) && !eksisterendeVeilederHarTilgangTilBruker(aktoerId);
    }

    boolean eksisterendeVeilederHarTilgangTilBruker(AktoerId aktoerId) {
        BrukerOppdatertInformasjon info = oppfolgingRepository.retrieveOppfolgingData(aktoerId).getOrElseThrow(ILLEGAL_STATE_EXCEPTION);
        String enhet = navKontorService.hentEnhetForBruker(aktoerId).getOrElseThrow(ILLEGAL_STATE_EXCEPTION);
        List<VeilederId> veilederePaaBrukerSinEnhet = veilederService.getIdenter(enhet);

        VeilederId eksisterendeVeileder = VeilederId.of(info.getVeileder());
        return veilederePaaBrukerSinEnhet.contains(eksisterendeVeileder);
    }

    static boolean brukerenIkkeLengerErUnderOppfolging(OppfolgingDTO dto) {
        return !dto.isOppfolging();
    }
}
