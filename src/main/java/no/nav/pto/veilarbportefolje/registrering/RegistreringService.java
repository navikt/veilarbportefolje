package no.nav.pto.veilarbportefolje.registrering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class RegistreringService extends KafkaCommonConsumerService<ArbeidssokerRegistrertEvent> {
    private final RegistreringRepositoryV2 registreringRepositoryV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Override
    public void behandleKafkaMeldingLogikk(ArbeidssokerRegistrertEvent kafkaMelding) {
        final AktorId aktoerId = AktorId.of(kafkaMelding.getAktorid());
        registreringRepositoryV2.upsertBrukerRegistrering(kafkaMelding);

        opensearchIndexerV2.updateRegistering(aktoerId, kafkaMelding);
        log.info("Oppdatert brukerregistrering for bruker: {}", aktoerId);
    }

    public void slettRegistering(AktorId aktoerId) {
        registreringRepositoryV2.slettBrukerRegistrering(aktoerId);
        log.info("Slettet brukerregistrering for bruker: {}", aktoerId);
    }
}
