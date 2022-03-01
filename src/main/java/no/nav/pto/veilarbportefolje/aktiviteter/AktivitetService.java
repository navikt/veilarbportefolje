package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktivitetService extends KafkaCommonConsumerService<KafkaAktivitetMelding> {
    private final AktivitetDAO aktivitetDAO;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final PersistentOppdatering persistentOppdatering;
    private final BrukerService brukerService;
    private final SisteEndringService sisteEndringService;
    private final OpensearchIndexer opensearchIndexer;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        sisteEndringService.behandleAktivitet(aktivitetData);

        //ORACLE
        AktorId aktorId = AktorId.of(aktivitetData.getAktorId());
        boolean bleProsessert = aktivitetDAO.tryLagreAktivitetData(aktivitetData);
        if (bleProsessert && aktivitetData.isAvtalt()) {
            // TODO: ved fjerning av oracle lagring. Dra ut oppdatering av mapping aktorId -> PersonId
            utledAktivitetstatuserForAktoerid(aktorId);
        }

        //POSTGRES
        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);
        //OPENSEARCH
        if (bleProsessert) {
            opensearchIndexer.indekser(aktorId);
        }
    }

    public void utledAktivitetstatuserForAktoerid(AktorId aktoerId) {
        AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerId, brukerService, aktivitetDAO);
        Optional.ofNullable(aktivitetBrukerOppdateringer)
                .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDB(oppdatering, aktoerId));
    }

    public void slettOgIndekserUtdanningsAktivitet(String aktivitetid, AktorId aktorId) {
        //ORACLE
        aktivitetDAO.deleteById(aktivitetid);
        utledAktivitetstatuserForAktoerid(aktorId);

        //POSTGRES
        aktiviteterRepositoryV2.deleteById(aktivitetid);

        //OPENSEARCH
        opensearchIndexer.indekser(aktorId);
    }

    public void upsertOgIndekserUtdanningsAktivitet(KafkaAktivitetMelding melding) {
        AktorId aktorId = AktorId.of(melding.getAktorId());
        //ORACLE
        aktivitetDAO.upsertAktivitet(melding);
        utledAktivitetstatuserForAktoerid(aktorId);

        //POSTGRES
        aktiviteterRepositoryV2.upsertAktivitet(melding);

        //OPENSEARCH
        opensearchIndexer.indekser(aktorId);
    }

    public void deaktiverUtgatteUtdanningsAktivteter(AktorId aktorId) {
        AktoerAktiviteter utdanningsAktiviteter = aktivitetDAO.getAktiviteterForAktoerid(aktorId);
        utdanningsAktiviteter.getAktiviteter()
                .stream()
                .filter(aktivitetDTO -> AktivitetTyperFraKafka.utdanningaktivitet.name().equals(aktivitetDTO.getAktivitetType()))
                .filter(aktivitetDTO -> aktivitetDTO.getTilDato().toLocalDateTime().isBefore(LocalDateTime.now()))
                .forEach(aktivitetDTO -> {
                            log.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}, på aktorId: {}", aktivitetDTO.getAktivitetID(), aktivitetDTO.getTilDato(), aktorId);
                            aktivitetDAO.setTilFullfort(aktivitetDTO.getAktivitetID());
                        }
                );
    }

    public void deaktiverUtgatteUtdanningsAktivteterPostgres() {
        List<AktivitetDTO> utdanningsAktiviteter = aktiviteterRepositoryV2.getPasserteAktiveUtdanningsAktiviter();
        log.info("Skal markere: {} utdanningsaktivteter som utgått", utdanningsAktiviteter.size());
        utdanningsAktiviteter.forEach(aktivitetDTO -> {
                    if(!AktivitetsType.utdanningaktivitet.name().equals(aktivitetDTO.getAktivitetType())){
                        log.error("Feil i utdanningsaktivteter sql!!!");
                        return;
                    }
                    log.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}", aktivitetDTO.getAktivitetID(), aktivitetDTO.getTilDato());
                    aktiviteterRepositoryV2.setTilFullfort(aktivitetDTO.getAktivitetID());
                }
        );
    }
}
