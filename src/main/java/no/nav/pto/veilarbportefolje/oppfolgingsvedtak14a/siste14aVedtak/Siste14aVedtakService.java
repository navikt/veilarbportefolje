package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.Gjeldende14aVedtak;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.Gjeldende14aVedtakService;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonNonKeyedConsumerService<Siste14aVedtakKafkaDto> {

    private final PdlIdentRepository pdlIdentRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final VedtaksstotteClient vedtaksstotteClient;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final Gjeldende14aVedtakService gjeldende14aVedtakService;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakKafkaDto kafkaMelding) {
        lagreSiste14aVedtak(Siste14aVedtakForBruker.fraKafkaDto(kafkaMelding));
    }

    public void lagreSiste14aVedtak(Siste14aVedtakForBruker siste14AVedtakForBruker) {
        if (pdlIdentRepository.erBrukerUnderOppfolging(siste14AVedtakForBruker.aktorId.get())) {
            AktorId aktorId = siste14AVedtakForBruker.getAktorId();

            IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(aktorId.get());
            siste14aVedtakRepository.upsert(siste14AVedtakForBruker, identer);

            Optional<Gjeldende14aVedtak> maybeGjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(aktorId);
            maybeGjeldende14aVedtak.ifPresent(gjeldende14aVedtak ->
                    opensearchIndexerV2.updateGjeldendeVedtak14a(new GjeldendeVedtak14a(
                            siste14AVedtakForBruker.getInnsatsgruppe(),
                            siste14AVedtakForBruker.getHovedmal(),
                            siste14AVedtakForBruker.getFattetDato()
                    ), aktorId)
            );
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
