package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static no.nav.common.json.JsonUtils.fromJson;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktivitetService extends KafkaCommonConsumerService<KafkaAktivitetMelding> implements KafkaConsumerService<String> {

    private final AktivitetDAO aktivitetDAO;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final AktivitetStatusRepositoryV2 prossesertAktivitetRepositoryV2;
    private final PersistentOppdatering persistentOppdatering;
    private final BrukerService brukerService;
    private final BrukerDataService brukerDataService;
    private final SisteEndringService sisteEndringService;
    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final ElasticIndexer elasticIndexer;
    private final AtomicBoolean rewind = new AtomicBoolean();

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);
        behandleKafkaMeldingLogikk(aktivitetData);
    }

    protected void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        log.info(
                "Behandler kafka-aktivtet-melding på aktorId: {} med aktivtetId: {}, version: {}",
                aktivitetData.getAktorId(),
                aktivitetData.getAktivitetId(),
                aktivitetData.getVersion()
        );
        AktorId aktorId = AktorId.of(aktivitetData.getAktorId());

        sisteEndringService.behandleAktivitet(aktivitetData);
        if (skallIkkeOppdatereAktivitet(aktivitetData)) {
            return;
        }

        boolean bleProsessert = aktivitetDAO.tryLagreAktivitetData(aktivitetData);
        if(bleProsessert){
            utledAktivitetstatuserForAktoerid(aktorId);
            elasticIndexer.indekser(aktorId);
            if (!oppfolgingRepository.erUnderoppfolging(aktorId)) {
                elasticServiceV2.deleteIfPresent(aktorId,
                        String.format("(AktivitetService) Sletter aktorId da brukeren ikke lengre er under oppfolging %s", aktivitetData.getAktorId()));
            }
        }

        //POSTGRES
        lagreOgProsseseserAktiviteter(aktivitetData);
    }

    public void lagreOgProsseseserAktiviteter(KafkaAktivitetMelding aktivitetData) {
        boolean bleProsessert = aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);

        if(bleProsessert){
            AktivitetStatus status = aktiviteterRepositoryV2.getAktivitetStatus(AktorId.of(aktivitetData.getAktorId()), aktivitetData.getAktivitetType());

            prossesertAktivitetRepositoryV2.upsertAktivitetTypeStatus(status, aktivitetData.getAktivitetType().name());
            brukerDataService.oppdaterAktivitetBrukerDataPostgres(AktorId.of(aktivitetData.getAktorId()));
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
        elasticIndexer.indekser(aktorId);

        //POSTGRES
        aktiviteterRepositoryV2.deleteById(aktivitetid);
        AktivitetStatus status = aktiviteterRepositoryV2.getAktivitetStatus(aktorId, KafkaAktivitetMelding.AktivitetTypeData.UTDANNINGAKTIVITET);
        prossesertAktivitetRepositoryV2.upsertAktivitetTypeStatus(status, AktivitetTyper.utdanningaktivitet.name());
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);
    }

    public void upsertOgIndekserAktiviteter(KafkaAktivitetMelding melding) {
        //ORACLE
        aktivitetDAO.upsertAktivitet(melding);
        utledAktivitetstatuserForAktoerid(AktorId.of(melding.getAktorId()));
        elasticIndexer.indekser(AktorId.of(melding.getAktorId()));

        //POSTGRES
        lagreOgProsseseserAktiviteter(melding);
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);
    }

    private boolean skallIkkeOppdatereAktivitet(KafkaAktivitetMelding aktivitetData) {
        return !aktivitetData.isAvtalt();
    }

    public void deaktiverUtgatteUtdanningsAktivteter(AktorId aktorId) {
        AktoerAktiviteter utdanningsAktiviteter = aktivitetDAO.getAvtalteAktiviteterForAktoerid(aktorId);
        utdanningsAktiviteter.getAktiviteter()
                .stream()
                .filter(aktivitetDTO -> AktivitetTyperFraKafka.utdanningaktivitet.name().equals(aktivitetDTO.getAktivitetType()))
                .filter(aktivitetDTO -> aktivitetDTO.getTilDato().toLocalDateTime().isBefore(LocalDateTime.now()))
                .forEach(aktivitetDTO -> {
                            log.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}, på aktorId: {}", aktivitetDTO.getAktivitetID(), aktivitetDTO.getTilDato(), aktorId);
                            aktivitetDAO.setAvtalt(aktivitetDTO.getAktivitetID(), false);
                        }
                );
    }
}
