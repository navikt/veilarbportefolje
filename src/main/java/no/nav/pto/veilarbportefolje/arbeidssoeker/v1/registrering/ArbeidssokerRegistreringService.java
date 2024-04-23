package no.nav.pto.veilarbportefolje.arbeidssoeker.v1.registrering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
@Slf4j
public class ArbeidssokerRegistreringService extends KafkaCommonConsumerService<ArbeidssokerRegistrertEvent> {
    private final ArbeidssokerRegistreringRepositoryV2 arbeidssokerRegistreringRepositoryV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Override
    public void behandleKafkaMeldingLogikk(ArbeidssokerRegistrertEvent kafkaMelding) {
        final AktorId aktoerId = AktorId.of(kafkaMelding.getAktorid());
        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(kafkaMelding);

        opensearchIndexerV2.updateRegistering(aktoerId, kafkaMelding);
        secureLog.info("Oppdatert brukerregistrering for bruker: {}", aktoerId);
    }

    public void slettRegistering(AktorId aktoerId) {
        arbeidssokerRegistreringRepositoryV2.slettBrukerRegistrering(aktoerId);
        secureLog.info("Slettet brukerregistrering for bruker: {}", aktoerId);
    }
}
