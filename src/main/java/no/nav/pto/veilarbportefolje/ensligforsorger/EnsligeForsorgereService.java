package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.VedtakOvergangsstønadArbeidsoppfølging;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerOvergangsstønadTiltak;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnsligeForsorgereService extends KafkaCommonConsumerService<VedtakOvergangsstønadArbeidsoppfølging> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final EnsligeForsorgereRepository ensligeForsorgereRepository;

    private final AktorClient aktorClient;

    @Override
    protected void behandleKafkaMeldingLogikk(VedtakOvergangsstønadArbeidsoppfølging melding) {
        secureLog.info("Oppdatere enslige forsorgere stønad for bruker: {}", melding.getPersonIdent());
        ensligeForsorgereRepository.lagreEnsligeForsorgereStonad(melding);

        Optional<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltakOptional = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(melding.getPersonIdent());
        AktorId aktorId = aktorClient.hentAktorId(Fnr.of(melding.getPersonIdent()));

        if (ensligeForsorgerOvergangsstønadTiltakOptional.isPresent()) {
            EnsligeForsorgerOvergangsstønadTiltak ensligeForsorgerOvergangsstønadTiltak = ensligeForsorgerOvergangsstønadTiltakOptional.get();
            Optional<LocalDate> yngsteBarn = ensligeForsorgereRepository.hentYngsteBarn(ensligeForsorgerOvergangsstønadTiltak.vedtakid());

            if (yngsteBarn.isEmpty()) {
                secureLog.warn("Kan ikke finne ef barn for vedtakId: " + ensligeForsorgerOvergangsstønadTiltak.vedtakid());
            }

            opensearchIndexerV2.updateOvergangsstonad(aktorId, new EnsligeForsorgerOvergangsstønadTiltakDto(
                    ensligeForsorgerOvergangsstønadTiltak.vedtaksPeriodetype(),
                    ensligeForsorgerOvergangsstønadTiltak.aktivitetsType(),
                    ensligeForsorgerOvergangsstønadTiltak.til_dato(),
                    yngsteBarn.orElse(null)
            ));
        } else {
            opensearchIndexerV2.deleteOvergansstonad(aktorId);
        }
    }
}
