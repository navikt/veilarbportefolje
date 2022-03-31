package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.oppfolging.response.SkjermingData;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingService {
    private final SkjermingRepository skjermingRepository;


    public void behandleSkjermedePersoner(ConsumerRecord<String, SkjermingDTO> kafkaMelding) {
        try {
            SkjermingDTO skjermingDTO = kafkaMelding.value();
            LocalDateTime skjermetFra = null;
            LocalDateTime skjermetTil = null;
            if (skjermingDTO.getSkjermetFra() != null && skjermingDTO.getSkjermetFra().length == 6) {
                skjermetFra = LocalDateTime.of(skjermingDTO.getSkjermetFra()[0], skjermingDTO.getSkjermetFra()[1], skjermingDTO.getSkjermetFra()[2], skjermingDTO.getSkjermetFra()[3], skjermingDTO.getSkjermetFra()[4], skjermingDTO.getSkjermetFra()[5]);
            }
            if (skjermingDTO.getSkjermetTil() != null && skjermingDTO.getSkjermetTil().length == 6) {
                skjermetTil = LocalDateTime.of(skjermingDTO.getSkjermetTil()[0], skjermingDTO.getSkjermetTil()[1], skjermingDTO.getSkjermetTil()[2], skjermingDTO.getSkjermetTil()[3], skjermingDTO.getSkjermetTil()[4], skjermingDTO.getSkjermetTil()[5]);
            }

            if (skjermetFra == null && skjermetTil == null) {
                throw new Exception("Possible illegal data about skjerming period, kafka message: " + kafkaMelding.value());
            }

            skjermingRepository.settSkjermingPeriode(Fnr.of(kafkaMelding.key()), DateUtils.toTimestamp(skjermetFra), DateUtils.toTimestamp(skjermetTil));
        } catch (Exception e) {
            log.error("Can't save skjerming periode " + e, e);
        }
    }

    public void behandleSkjermingStatus(ConsumerRecord<String, String> kafkaMelding) {
        try {
            Boolean erSkjermet = Boolean.valueOf(kafkaMelding.value());

            if (erSkjermet) {
                skjermingRepository.settSkjerming(Fnr.of(kafkaMelding.key()), erSkjermet);
            } else {
                skjermingRepository.deleteSkjermingData(Fnr.of(kafkaMelding.key()));
            }
        } catch (Exception e) {
            log.error("Can't save skjerming status " + e, e);
        }
    }

    public Optional<SkjermingData> hentSkjermingData(String fnr) {
        Optional<SkjermingData> skjermingData = skjermingRepository.hentSkjermingData(Fnr.of(fnr));

        if (skjermingData.isPresent() && !skjermingData.get().isEr_skjermet()) {
            skjermingRepository.deleteSkjermingData(Fnr.of(fnr));
            return Optional.empty();
        }
        return skjermingData;
    }

    public Set<Fnr> hentSkjermetPersoner(List<String> fnr) {
        List<Fnr> fnrs = fnr.stream().map(Fnr::of).collect(Collectors.toList());

        Optional<Set<Fnr>> skjermetPersoner = skjermingRepository.hentSkjermetPersoner(fnrs);

        if (skjermetPersoner.isEmpty()) {
            throw new RuntimeException("Can't get skjermet personer");
        }
        return skjermetPersoner.get();
    }
}
