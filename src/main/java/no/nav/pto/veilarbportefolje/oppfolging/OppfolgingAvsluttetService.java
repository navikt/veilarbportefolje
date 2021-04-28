package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.CvRepository;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

import static java.time.Instant.EPOCH;
import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.ofInstant;

@Service
@RequiredArgsConstructor
public class OppfolgingAvsluttetService implements KafkaConsumerService<String> {
    private static final Logger log = LoggerFactory.getLogger(OppfolgingAvsluttetService.class);

    private final ArbeidslisteService arbeidslisteService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final RegistreringService registreringService;
    private final CvRepository cvRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final SisteEndringService sisteEndringService;

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final OppfolgingAvsluttetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingAvsluttetDTO.class);
        final AktorId aktoerId = dto.getAktorId();


        // TODO: bruk toggle for oppfolgingRepositoryV2
        final ZonedDateTime startDato = oppfolgingRepository.hentStartdato(aktoerId).orElse(ofInstant(EPOCH, of("Europe/Oslo")));

        final ZonedDateTime sluttDato = dto.getSluttdato();
        if (startDato.isAfter(sluttDato)) {
            log.warn("Lagret startdato for oppfølging er etter mottatt sluttdato for bruker {}", aktoerId);
            return;
        }

        avsluttOppfolging(aktoerId);
    }

    public void avsluttOppfolging(AktorId aktoerId) {
        oppfolgingRepository.slettOppfolgingData(aktoerId);
        oppfolgingRepositoryV2.slettOppfolgingData(aktoerId);
        registreringService.slettRegistering(aktoerId);
        arbeidslisteService.slettArbeidsliste(aktoerId);
        sisteEndringService.slettSisteEndringer(aktoerId);
        cvRepository.slettCVData(aktoerId);

        elasticServiceV2.slettDokumenter(List.of(aktoerId));
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
