package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltakshendelseService extends KafkaCommonConsumerService<KafkaTiltakshendelse> {
    private final TiltakshendelseRepository tiltakshendelseRepository;
    private final BrukerServiceV2 brukerServiceV2;

    @Override
    protected void behandleKafkaMeldingLogikk(KafkaTiltakshendelse tiltakshendelseData) {

        AktorId aktorId = brukerServiceV2.hentAktorId(tiltakshendelseData.fnr())
                .orElseThrow(() -> new RuntimeException("Kunne ikke hente aktørid for fnr"));
        boolean bleLagret = tiltakshendelseRepository.tryLagreTiltakshendelseData(tiltakshendelseData);

        if (bleLagret) {
            // TODO Indekser i opensearch
        }
    }
}
