package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.oppfolging.response.SkjermingData;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

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

            skjermingRepository.settSkjermingPeriode(Fnr.of(kafkaMelding.key()), DateUtils.toTimestamp(skjermetFra), DateUtils.toTimestamp(skjermetTil));
        } catch (Exception e) {
            log.error("Can't save skjerming periode " + e, e);
        }
    }

    public void behandleSkjermingStatus(ConsumerRecord<String, String> kafkaMelding) {
        try {
            Boolean erSkjermet = Boolean.valueOf(kafkaMelding.value());

            skjermingRepository.settSkjerming(Fnr.of(kafkaMelding.key()), erSkjermet);
        } catch (Exception e) {
            log.error("Can't save skjerming status " + e, e);
        }
    }

    public Optional<SkjermingData> hentSkjermingData(Fnr fnr) {
        return skjermingRepository.hentSkjermingData(fnr);
    }
}
