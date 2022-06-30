package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukNOMSkjerming;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingService {
    private final SkjermingRepository skjermingRepository;
    private final AktorClient aktorClient;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final UnleashService unleashService;

    @SneakyThrows
    public void behandleSkjermedePersoner(ConsumerRecord<String, SkjermingDTO> kafkaMelding) {
        Fnr fnr = Fnr.of(kafkaMelding.key());
        SkjermingDTO skjermingDTO = kafkaMelding.value();
        LocalDateTime skjermetFra = null;
        LocalDateTime skjermetTil = null;
        if (skjermingDTO.getSkjermetFra() != null && skjermingDTO.getSkjermetFra().length >= 5) {
            skjermetFra = LocalDateTime.of(skjermingDTO.getSkjermetFra()[0], skjermingDTO.getSkjermetFra()[1], skjermingDTO.getSkjermetFra()[2], skjermingDTO.getSkjermetFra()[3], skjermingDTO.getSkjermetFra()[4], 0);
        }
        if (skjermingDTO.getSkjermetTil() != null && skjermingDTO.getSkjermetTil().length >= 5) {
            skjermetTil = LocalDateTime.of(skjermingDTO.getSkjermetTil()[0], skjermingDTO.getSkjermetTil()[1], skjermingDTO.getSkjermetTil()[2], skjermingDTO.getSkjermetTil()[3], skjermingDTO.getSkjermetTil()[4], 0);
        }

        if (skjermetFra == null && skjermetTil == null) {
            throw new Exception("Possible illegal data about skjerming period, kafka message: " + kafkaMelding.value());
        }

        skjermingRepository.settSkjermingPeriode(fnr, DateUtils.toTimestamp(skjermetFra), DateUtils.toTimestamp(skjermetTil));

        if (brukNOMSkjerming(unleashService)) {
            String aktorId = aktorClient.hentAktorId(fnr).get();
            opensearchIndexerV2.updateSkjermetTil(AktorId.of(aktorId), skjermetTil);
        }
    }

    public void behandleSkjermingStatus(ConsumerRecord<String, String> kafkaMelding) {
        Fnr fnr = Fnr.of(kafkaMelding.key());
        Boolean erSkjermet = Boolean.valueOf(kafkaMelding.value());

        if (erSkjermet) {
            skjermingRepository.settSkjerming(fnr, erSkjermet);
        } else {
            skjermingRepository.deleteSkjermingData(fnr);
        }

        if (brukNOMSkjerming(unleashService)) {
            String aktorId = aktorClient.hentAktorId(fnr).get();
            opensearchIndexerV2.updateErSkjermet(AktorId.of(aktorId), erSkjermet);
        }
    }

    public Set<Fnr> hentSkjermetPersoner(List<String> fnr) {
        List<Fnr> fnrs = fnr.stream().map(Fnr::of).collect(Collectors.toList());
        return skjermingRepository.hentSkjermetPersoner(fnrs);
    }
}
