package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukIkkeAvtalteAktiviteter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktivitetService extends KafkaCommonConsumerService<KafkaAktivitetMelding> {

    private final AktivitetDAO aktivitetDAO;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final AktivitetStatusRepositoryV2 prossesertAktivitetRepositoryV2;
    private final PersistentOppdatering persistentOppdatering;
    private final BrukerService brukerService;
    private final BrukerDataService brukerDataService;
    private final SisteEndringService sisteEndringService;
    private final UnleashService unleashService;
    private final ElasticIndexer elasticIndexer;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        log.info(
                "Behandler kafka-aktivtet-melding på aktorId: {} med aktivtetId: {}, version: {}",
                aktivitetData.getAktorId(),
                aktivitetData.getAktivitetId(),
                aktivitetData.getVersion()
        );
        sisteEndringService.behandleAktivitet(aktivitetData);

        //ORACLE
        if (aktivitetData.getVersion() > 49179897) {
            AktorId aktorId = AktorId.of(aktivitetData.getAktorId());
            boolean bleProsessert = aktivitetDAO.tryLagreAktivitetData(aktivitetData);

            if (bleProsessert && (aktivitetData.isAvtalt() || brukIkkeAvtalteAktiviteter(unleashService))) {
                utledAktivitetstatuserForAktoerid(aktorId);
                elasticIndexer.indekser(aktorId);
            }
        }
        //POSTGRES
        boolean bleProsessertPostgres = aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);
        if (bleProsessertPostgres && (aktivitetData.isAvtalt() || brukIkkeAvtalteAktiviteter(unleashService))) {
            utleddAktivitetStatuser(AktorId.of(aktivitetData.getAktorId()), aktivitetData.getAktivitetType());
        }
    }

    public void utleddAktivitetStatuser(AktorId aktorId, KafkaAktivitetMelding.AktivitetTypeData aktivitetType) {
        AktivitetStatus status = aktiviteterRepositoryV2.getAktivitetStatus(aktorId, aktivitetType, brukIkkeAvtalteAktiviteter(unleashService));
        prossesertAktivitetRepositoryV2.upsertAktivitetTypeStatus(status, aktivitetType.name());
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
    }

    public void utledAktivitetstatuserForAktoerid(AktorId aktoerId) {
        AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerId, brukerService, aktivitetDAO, brukIkkeAvtalteAktiviteter(unleashService));
        Optional.ofNullable(aktivitetBrukerOppdateringer)
                .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDB(oppdatering, aktoerId));
    }

    public void slettOgIndekserUtdanningsAktivitet(String aktivitetid, AktorId aktorId) {
        //ORACLE
        aktivitetDAO.deleteById(aktivitetid);
        utledAktivitetstatuserForAktoerid(aktorId);
        elasticIndexer.indekser(aktorId);

        //POSTGRES
        aktiviteterRepositoryV2.deleteById(aktivitetid);
        AktivitetStatus status = aktiviteterRepositoryV2.getAktivitetStatus(aktorId, KafkaAktivitetMelding.AktivitetTypeData.UTDANNINGAKTIVITET, brukIkkeAvtalteAktiviteter(unleashService));
        prossesertAktivitetRepositoryV2.upsertAktivitetTypeStatus(status, AktivitetTyper.utdanningaktivitet.name());
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
    }

    public void upsertOgIndekserAktiviteter(KafkaAktivitetMelding melding) {
        //ORACLE
        aktivitetDAO.upsertAktivitet(melding);
        utledAktivitetstatuserForAktoerid(AktorId.of(melding.getAktorId()));
        elasticIndexer.indekser(AktorId.of(melding.getAktorId()));

        //POSTGRES
        aktiviteterRepositoryV2.upsertAktivitet(melding);
        utleddAktivitetStatuser(AktorId.of(melding.getAktorId()), melding.getAktivitetType());
    }

    public void deaktiverUtgatteUtdanningsAktivteter(AktorId aktorId) {
        AktoerAktiviteter utdanningsAktiviteter = aktivitetDAO.getAktiviteterForAktoerid(aktorId, brukIkkeAvtalteAktiviteter(unleashService));
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
}
