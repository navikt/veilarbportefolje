package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.Result;

import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class VedtakService implements KafkaConsumerService<String> {

    private final VedtakStatusRepository vedtakStatusRepository;
    private final ElasticIndexer elasticIndexer;
    private final AktoerService aktoerService;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
    }

    public Result<String> behandleKafkaMelding(String melding) {
        KafkaVedtakStatusEndring vedtakStatusEndring = fromJson(melding, KafkaVedtakStatusEndring.class);
        KafkaVedtakStatusEndring.VedtakStatusEndring vedtakStatus = vedtakStatusEndring.getVedtakStatusEndring();
        return Result.of(() -> {

            switch (vedtakStatus) {
                case UTKAST_SLETTET: {
                    slettUtkast(vedtakStatusEndring);
                    return melding;
                }
                case VEDTAK_SENDT: {
                    setVedtakSendt(vedtakStatusEndring);
                    return melding;
                }
                case UTKAST_OPPRETTET:
                case BESLUTTER_PROSESS_STARTET:
                case BLI_BESLUTTER:
                case GODKJENT_AV_BESLUTTER:
                case KLAR_TIL_BESLUTTER:
                case KLAR_TIL_VEILEDER: {
                    oppdaterUtkast(vedtakStatusEndring);
                    return melding;
                }
                default:
                    throw new IllegalStateException();
            }

        });
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        indekserBruker(AktoerId.of(melding.getAktorId()));
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        indekserBruker(AktoerId.of(melding.getAktorId()));
    }


    private void setVedtakSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        indekserBruker(AktoerId.of(melding.getAktorId()));
    }

    private void indekserBruker(AktoerId aktoerId) {
        Result<OppfolgingsBruker> result = elasticIndexer.indekser(aktoerId)
                .mapError(err -> {
                            Fnr fnr = aktoerService.hentFnrFraAktorId(aktoerId).get();
                            return elasticIndexer.indekser(fnr);
                        }
                );
        if (result.isErr()) {
            log.warn("Feil ved indeksering av bruker med aktorId {}", aktoerId.aktoerId);
        }
    }
}
