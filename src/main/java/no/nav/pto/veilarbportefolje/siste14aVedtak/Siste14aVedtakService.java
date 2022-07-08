package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonConsumerService<Siste14aVedtakDTO> {

    private final PdlIdentRepository pdlIdentRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakDTO kafkaMelding) {
        lagreSiste14aVedtak(kafkaMelding);
    }

    public void lagreSiste14aVedtak(Siste14aVedtakDTO siste14aVedtakDTO) {
        if (pdlIdentRepository.harAktorIdUnderOppfolging(siste14aVedtakDTO.aktorId)) {
            IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(siste14aVedtakDTO.aktorId);
            siste14aVedtakRepository.upsert(siste14aVedtakDTO, identer);
        }
    }

    public void slettSiste14aVedtak(AktorId aktorId) {
        IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(aktorId);
        siste14aVedtakRepository.delete(identer);
    }
}
