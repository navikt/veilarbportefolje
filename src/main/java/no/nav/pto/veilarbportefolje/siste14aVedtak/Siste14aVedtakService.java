package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonConsumerService<Siste14aVedtakDTO> {

    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final Siste14aVedtakRepository siste14aVedtakRepository;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakDTO kafkaMelding) {
        lagreSiste14aVedtak(kafkaMelding);
    }

    public void lagreSiste14aVedtak(Siste14aVedtakDTO siste14aVedtakDTO) {
        if (oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(siste14aVedtakDTO.aktorId)) {
            siste14aVedtakRepository.upsert(siste14aVedtakDTO);
        }
    }

    public void slettSiste14aVedtak(AktorId aktorId) {
        siste14aVedtakRepository.delete(aktorId);
    }
}
