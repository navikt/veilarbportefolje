package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GruppeAktivitetService {
    private final GruppeAktivitetRepository gruppeAktivitetRepository;
    private final GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2;
    private final AktorClient aktorClient;
    private final BrukerDataService brukerDataService;
    private final ElasticIndexer elasticIndexer;

    public void behandleKafkaRecord(ConsumerRecord<String, GruppeAktivitetDTO> kafkaMelding) {
        GruppeAktivitetDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMeldingOracle(melding);
        behandleKafkaMeldingPostgres(melding);
    }

    public void behandleKafkaMeldingOracle(GruppeAktivitetDTO kafkaMelding) {
        GruppeAktivitetInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        PersonId personId = PersonId.of(String.valueOf(innhold.getPersonId()));
        boolean aktiv = !(skalSlettesGoldenGate(kafkaMelding) || skalSletteGruppeAktivitet(innhold));
        gruppeAktivitetRepository.upsertGruppeAktivitet(innhold, aktorId, aktiv);
        gruppeAktivitetRepository.utledOgLagreGruppeaktiviteter(aktorId, personId);
        brukerDataService.oppdaterAktivitetBrukerData(aktorId, personId);

        elasticIndexer.indekser(aktorId);
    }


    public void behandleKafkaMeldingPostgres(GruppeAktivitetDTO kafkaMelding) {
        GruppeAktivitetInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMeldingV2(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        boolean aktiv = !(skalSlettesGoldenGate(kafkaMelding) || skalSletteGruppeAktivitet(innhold));
        gruppeAktivitetRepositoryV2.upsertGruppeAktivitet(innhold, aktorId, aktiv);
        gruppeAktivitetRepositoryV2.utledOgLagreGruppeaktiviteter(aktorId);

        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
    }


    private boolean skalSletteGruppeAktivitet(GruppeAktivitetInnhold gruppeInnhold) {
        return gruppeInnhold.getAktivitetperiodeTil() == null || erUtgatt(gruppeInnhold.getAktivitetperiodeTil(), true);
    }

    private boolean erGammelMelding(GruppeAktivitetDTO kafkaMelding, GruppeAktivitetInnhold innhold){
        Long hendelseIDB = gruppeAktivitetRepository.retrieveHendelse(innhold).orElse(-1L);
        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            log.info("Fikk tilsendt gammel gruppe-aktivtet-melding pa Oracle");
            return true;
        }
        return false;
    }

    private boolean erGammelMeldingV2(GruppeAktivitetDTO kafkaMelding, GruppeAktivitetInnhold innhold){
        Long hendelseIDB = gruppeAktivitetRepositoryV2.retrieveHendelse(innhold).orElse(-1L);
        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            log.info("Fikk tilsendt gammel gruppe-aktivtet-melding pa Posrgres");
            return true;
        }
        return false;
    }
}
