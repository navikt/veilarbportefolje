package no.nav.pto.veilarbportefolje.skjerming;

import lombok.SneakyThrows;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
public class SkjermingService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SkjermingService.class);
    private final SkjermingRepository skjermingRepository;
    private final BrukerServiceV2 brukerService;
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;

    @java.beans.ConstructorProperties({"skjermingRepository", "brukerService", "opensearchIndexerPaDatafelt"})
    public SkjermingService(SkjermingRepository skjermingRepository, BrukerServiceV2 brukerService, OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt) {
        this.skjermingRepository = skjermingRepository;
        this.brukerService = brukerService;
        this.opensearchIndexerPaDatafelt = opensearchIndexerPaDatafelt;
    }

    @SneakyThrows
    public void behandleSkjermedePersoner(ConsumerRecord<String, SkjermingDTO> kafkaMelding) {
        Fnr fnr = Fnr.of(kafkaMelding.key());
        SkjermingDTO skjermingDTO = kafkaMelding.value();
        LocalDateTime skjermetFra;
        LocalDateTime skjermetTil;
        if (isDevelopment().orElse(false) && skjermingDTO == null) {
            secureLog.info(String.format("Ignorerer dårlig datakvalitet i dev, bruker fnr %s, kafka melding: %s",
                    fnr.get(),
                    kafkaMelding.value()));
            return;
        }

        if (skjermingDTO.skjermetFra() != null && skjermingDTO.skjermetFra().length >= 5) {
            skjermetFra = LocalDateTime.of(skjermingDTO.skjermetFra()[0],
                    skjermingDTO.skjermetFra()[1],
                    skjermingDTO.skjermetFra()[2],
                    skjermingDTO.skjermetFra()[3],
                    skjermingDTO.skjermetFra()[4],
                    0);
        } else {
            skjermetFra = null;
        }
        if (skjermingDTO.skjermetTil() != null && skjermingDTO.skjermetTil().length >= 5) {
            skjermetTil = LocalDateTime.of(skjermingDTO.skjermetTil()[0],
                    skjermingDTO.skjermetTil()[1],
                    skjermingDTO.skjermetTil()[2],
                    skjermingDTO.skjermetTil()[3],
                    skjermingDTO.skjermetTil()[4],
                    0);
        } else {
            skjermetTil = null;
        }

        if (skjermetFra == null && skjermetTil == null) {
            throw new Exception("Possible illegal data about skjerming period, kafka message: " + kafkaMelding.value());
        }

        skjermingRepository.settSkjermingPeriode(fnr,
                DateUtils.toTimestamp(skjermetFra),
                DateUtils.toTimestamp(skjermetTil));

        brukerService.hentAktorId(fnr).ifPresent(aktorId ->
                opensearchIndexerPaDatafelt.updateSkjermetTil(aktorId, skjermetTil)
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
                opensearchIndexerPaDatafelt.updateErSkjermet(aktorId, erSkjermet)
        );
    }
}
