package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonConsumerService<Siste14aVedtakKafkaDto> {

    private final PdlIdentRepository pdlIdentRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final VedtaksstotteClient vedtaksstotteClient;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakKafkaDto kafkaMelding) {
        lagreSiste14aVedtak(Siste14aVedtakForBruker.fraKafkaDto(kafkaMelding));
    }

    public void lagreSiste14aVedtak(Siste14aVedtakForBruker siste14AVedtakForBruker) {
        if (pdlIdentRepository.erBrukerUnderOppfolging(siste14AVedtakForBruker.aktorId.get())) {
            IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(siste14AVedtakForBruker.aktorId.get());
            siste14aVedtakRepository.upsert(siste14AVedtakForBruker, identer);

            opensearchIndexerV2.updateGjeldendeVedtak14a(new GjeldendeVedtak14a(
                    siste14AVedtakForBruker.getInnsatsgruppe(),
                    siste14AVedtakForBruker.getHovedmal(),
                    siste14AVedtakForBruker.getFattetDato()
            ), siste14AVedtakForBruker.getAktorId());
        }
    }

    public void slettSiste14aVedtak(String brukerId) {
        IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(brukerId);
        siste14aVedtakRepository.delete(identer);
    }

    public void hentOgLagreSiste14aVedtak(AktorId aktorId) {
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktorId);

        vedtaksstotteClient.hentSiste14aVedtak(fnr)
                .map(siste14aVedtak -> Siste14aVedtakForBruker.fraApiDto(siste14aVedtak, aktorId))
                .ifPresent(this::lagreSiste14aVedtak);
    }
}
