package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.PortefoljeMapper;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonConsumerService<Siste14aVedtakKafkaDto> {

    private final PdlIdentRepository pdlIdentRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final VedtaksstotteClient vedtaksstotteClient;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakKafkaDto kafkaMelding) {
        lagreSiste14aVedtak(Siste14aVedtak.fraKafkaDto(kafkaMelding));
    }

    public void lagreSiste14aVedtak(Siste14aVedtak siste14aVedtak) {
        if (pdlIdentRepository.erBrukerUnderOppfolging(siste14aVedtak.brukerId)) {
            IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(siste14aVedtak.brukerId);
            siste14aVedtakRepository.upsert(siste14aVedtak, identer);
            if(!siste14aVedtak.fraArena) {
                String hovedmaal = ofNullable(siste14aVedtak.hovedmal).map(PortefoljeMapper::mapTilArenaHovedmal).map(ArenaHovedmal::name).orElse(null);
                opensearchIndexerV2.updateHovedmaalkode(AktorId.of(siste14aVedtak.brukerId), hovedmaal);
            }
        }
    }

    public void slettSiste14aVedtak(String brukerId) {
        IdenterForBruker identer = pdlIdentRepository.hentIdenterForBruker(brukerId);
        siste14aVedtakRepository.delete(identer);
    }

    public void hentOgLagreSiste14aVedtak(AktorId aktorId) {
        Fnr fnr = pdlIdentRepository.hentFnr(aktorId);

        vedtaksstotteClient.hentSiste14aVedtak(fnr)
                .map(siste14aVedtak -> Siste14aVedtak.fraApiDto(siste14aVedtak, aktorId.get()))
                .ifPresent(this::lagreSiste14aVedtak);
    }
}
