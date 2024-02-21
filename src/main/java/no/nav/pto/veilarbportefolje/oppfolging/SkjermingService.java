package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.interfaces.HandtereOppfolgingData;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingService implements HandtereOppfolgingData<Fnr> {
    private final SkjermingRepository skjermingRepository;
    private final BrukerServiceV2 brukerService;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @SneakyThrows
    public void behandleSkjermedePersoner(ConsumerRecord<String, SkjermingDTO> kafkaMelding) {
        Fnr fnr = Fnr.of(kafkaMelding.key());
        SkjermingDTO skjermingDTO = kafkaMelding.value();
        LocalDateTime skjermetFra;
        LocalDateTime skjermetTil;
        if (skjermingDTO.getSkjermetFra() != null && skjermingDTO.getSkjermetFra().length >= 5) {
            skjermetFra = LocalDateTime.of(skjermingDTO.getSkjermetFra()[0], skjermingDTO.getSkjermetFra()[1], skjermingDTO.getSkjermetFra()[2], skjermingDTO.getSkjermetFra()[3], skjermingDTO.getSkjermetFra()[4], 0);
        } else {
            skjermetFra = null;
        }
        if (skjermingDTO.getSkjermetTil() != null && skjermingDTO.getSkjermetTil().length >= 5) {
            skjermetTil = LocalDateTime.of(skjermingDTO.getSkjermetTil()[0], skjermingDTO.getSkjermetTil()[1], skjermingDTO.getSkjermetTil()[2], skjermingDTO.getSkjermetTil()[3], skjermingDTO.getSkjermetTil()[4], 0);
        } else {
            skjermetTil = null;
        }

        if (skjermetFra == null && skjermetTil == null) {
            throw new Exception("Possible illegal data about skjerming period, kafka message: " + kafkaMelding.value());
        }

        skjermingRepository.settSkjermingPeriode(fnr, DateUtils.toTimestamp(skjermetFra), DateUtils.toTimestamp(skjermetTil));

        brukerService.hentAktorId(fnr).ifPresent(aktorId ->
                opensearchIndexerV2.updateSkjermetTil(aktorId, skjermetTil)
        );
    }

    public void behandleSkjermingStatus(ConsumerRecord<String, String> kafkaMelding) {
        Fnr fnr = Fnr.of(kafkaMelding.key());
        boolean erSkjermet = kafkaMelding.value() != null && Boolean.parseBoolean(kafkaMelding.value());

        if (erSkjermet) {
            skjermingRepository.settSkjerming(fnr, true);
        } else {
            skjermingRepository.deleteSkjermingData(fnr);
        }

        brukerService.hentAktorId(fnr).ifPresent(aktorId ->
                opensearchIndexerV2.updateErSkjermet(aktorId, erSkjermet)
        );
    }

    public void slettOppfolgingData(Fnr fnr) {
        skjermingRepository.deleteSkjermingData(fnr);
    }


}
