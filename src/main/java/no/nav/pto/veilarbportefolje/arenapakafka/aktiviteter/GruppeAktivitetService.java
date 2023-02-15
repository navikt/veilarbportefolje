package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.*;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class GruppeAktivitetService {
    private final GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;
    private final AktorClient aktorClient;

    public void behandleKafkaRecord(ConsumerRecord<String, GruppeAktivitetDTO> kafkaMelding) {
        GruppeAktivitetDTO melding = kafkaMelding.value();
        secureLog.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMelding(melding);
    }

    public void behandleKafkaMelding(GruppeAktivitetDTO melding) {
        GruppeAktivitetInnhold innhold = getInnhold(melding);
        if (innhold == null || erGammelMelding(melding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        boolean aktiv = !(skalSlettesGoldenGate(melding) || skalSletteGruppeAktivitet(innhold));
        gruppeAktivitetRepositoryV2.upsertGruppeAktivitet(innhold, aktorId, aktiv);

        opensearchIndexer.indekser(aktorId);
    }

    private boolean skalSletteGruppeAktivitet(GruppeAktivitetInnhold gruppeInnhold) {
        return gruppeInnhold.getAktivitetperiodeTil() == null || erUtgatt(gruppeInnhold.getAktivitetperiodeTil(), true);
    }

    private boolean erGammelMelding(GruppeAktivitetDTO kafkaMelding, GruppeAktivitetInnhold innhold) {
        Long hendelseIDB = gruppeAktivitetRepositoryV2.retrieveHendelse(innhold).orElse(-1L);
        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            log.info("Fikk tilsendt gammel gruppe-aktivtet-melding pa Posrgres");
            return true;
        }
        return false;
    }
}
