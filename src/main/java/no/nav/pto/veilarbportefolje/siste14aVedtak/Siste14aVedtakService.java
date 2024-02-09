package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.interfaces.HandtereOppfolgingData;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonConsumerService<Siste14aVedtakKafkaDto> implements HandtereOppfolgingData<AktorId> {

    private final PdlIdentRepository pdlIdentRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final VedtaksstotteClient vedtaksstotteClient;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakKafkaDto kafkaMelding) {
        lagreSiste14aVedtak(Siste14aVedtak.fraKafkaDto(kafkaMelding));
    }

    public void lagreSiste14aVedtak(Siste14aVedtak siste14aVedtak) {
        if (pdlIdentRepository.erBrukerUnderOppfolging(siste14aVedtak.brukerId)) {
            IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(siste14aVedtak.brukerId);
            siste14aVedtakRepository.upsert(siste14aVedtak, identer);
        }
    }

    public void slettOppfolgingData(AktorId aktorId) {
        IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(aktorId.get());
        siste14aVedtakRepository.delete(identer);
    }

    public void hentOgLagreSiste14aVedtak(AktorId aktorId) {
        Fnr fnr = pdlIdentRepository.hentFnr(aktorId);

        vedtaksstotteClient.hentSiste14aVedtak(fnr)
                .map(siste14aVedtak -> Siste14aVedtak.fraApiDto(siste14aVedtak, aktorId.get()))
                .ifPresent(this::lagreSiste14aVedtak);
    }
}
