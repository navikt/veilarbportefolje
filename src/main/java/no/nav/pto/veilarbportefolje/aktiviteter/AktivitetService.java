package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    private final OpensearchIndexer opensearchIndexer;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        log.info(
                "Behandler kafka-aktivtet-melding på aktorId: {} med aktivtetId: {}, version: {}",
                aktivitetData.getAktorId(),
                aktivitetData.getAktivitetId(),
                aktivitetData.getVersion()
        );
        sisteEndringService.behandleAktivitet(aktivitetData);

        //ORACLE
        AktorId aktorId = AktorId.of(aktivitetData.getAktorId());
        boolean bleProsessert = aktivitetDAO.tryLagreAktivitetData(aktivitetData);

        if (bleProsessert && (aktivitetData.isAvtalt() || brukIkkeAvtalteAktiviteter(unleashService))) {
            utledAktivitetstatuserForAktoerid(aktorId);
        }

        //POSTGRES
        boolean bleProsessertPostgres = aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);
        if (bleProsessertPostgres && (aktivitetData.isAvtalt() || brukIkkeAvtalteAktiviteter(unleashService))) {
            oppdaterAktivitetTypeStatus(AktorId.of(aktivitetData.getAktorId()), aktivitetData.getAktivitetType());
            brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
        }

        //OPENSEARCH
        if(bleProsessert && oppfolgingRepositoryV2.erUnderOppfolging(aktorId)){
            opensearchIndexer.indekser(aktorId);
        }
    }

    public void utledAktivitetstatuserForAktoerid(AktorId aktoerId) {
        AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerId, brukerService, aktivitetDAO, brukIkkeAvtalteAktiviteter(unleashService));
        Optional.ofNullable(aktivitetBrukerOppdateringer)
                .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDB(oppdatering, aktoerId));
    }

    public void utledAktivitetstatuserForAktoeridPostgres(AktorId aktoerId) {
        Arrays.stream(KafkaAktivitetMelding.AktivitetTypeData.values()).forEach(type -> oppdaterAktivitetTypeStatus(aktoerId, type));
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktoerId);
    }

    public void slettOgIndekserUtdanningsAktivitet(String aktivitetid, AktorId aktorId) {
        //ORACLE
        aktivitetDAO.deleteById(aktivitetid);
        utledAktivitetstatuserForAktoerid(aktorId);
        opensearchIndexer.indekser(aktorId);

        //POSTGRES
        aktiviteterRepositoryV2.deleteById(aktivitetid);
        oppdaterAktivitetTypeStatus(aktorId, KafkaAktivitetMelding.AktivitetTypeData.UTDANNINGAKTIVITET);
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
    }

    public void oppdaterAktivitetTypeStatus(AktorId aktorId, KafkaAktivitetMelding.AktivitetTypeData type) {
        AktivitetStatus status = aktiviteterRepositoryV2.getAktivitetStatus(aktorId, type, brukIkkeAvtalteAktiviteter(unleashService));
        prossesertAktivitetRepositoryV2.upsertAktivitetTypeStatus(status, type.name().toLowerCase());
    }

    public void upsertOgIndekserAktiviteter(KafkaAktivitetMelding melding) {
        AktorId aktorId = AktorId.of(melding.getAktorId());
        //ORACLE
        aktivitetDAO.upsertAktivitet(melding);
        utledAktivitetstatuserForAktoerid(aktorId);
        opensearchIndexer.indekser(aktorId);

        //POSTGRES
        aktiviteterRepositoryV2.upsertAktivitet(melding);
        oppdaterAktivitetTypeStatus(aktorId, melding.getAktivitetType());
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
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

    public void deaktiverUtgatteUtdanningsAktivteterPostgres(AktorId aktorId) {
        AktoerAktiviteter utdanningsAktiviteter = aktiviteterRepositoryV2.getAktiviteterForAktoerid(aktorId, brukIkkeAvtalteAktiviteter(unleashService));
        utdanningsAktiviteter.getAktiviteter()
                .stream()
                .filter(aktivitetDTO -> AktivitetTyperFraKafka.utdanningaktivitet.name().equals(aktivitetDTO.getAktivitetType()))
                .filter(aktivitetDTO -> aktivitetDTO.getTilDato().toLocalDateTime().isBefore(LocalDateTime.now()))
                .forEach(aktivitetDTO -> {
                            log.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}, på aktorId: {}", aktivitetDTO.getAktivitetID(), aktivitetDTO.getTilDato(), aktorId);
                            aktiviteterRepositoryV2.setTilFullfort(aktivitetDTO.getAktivitetID());
                        }
                );
    }
}
