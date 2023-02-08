package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.VedtakOvergangsstønadArbeidsoppfølging;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnsligeForsorgereService extends KafkaCommonConsumerService<VedtakOvergangsstønadArbeidsoppfølging> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final EnsligeForsorgereRepository ensligeForsorgereRepository;

    @Override
    protected void behandleKafkaMeldingLogikk(VedtakOvergangsstønadArbeidsoppfølging melding) {
        log.info("Oppdatert overgangsstonad for bruker: {}", melding.getPersonIdent());
        opensearchIndexerV2.updateOvergangsstonad(melding);
    }
}
