package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

@Slf4j
@Service
public class AktivitetService implements KafkaConsumerService<String> {

    private final BrukerService brukerService;
    private final AktivitetDAO aktivitetDAO;
    private final PersistentOppdatering persistentOppdatering;
    private final AtomicBoolean rewind;
    private final SisteEndringService sisteEndringService;

    @Autowired
    public AktivitetService(AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering, BrukerService brukerService, SisteEndringService sisteEndringService) {
        this.aktivitetDAO = aktivitetDAO;
        this.brukerService = brukerService;
        this.persistentOppdatering = persistentOppdatering;
        this.sisteEndringService = sisteEndringService;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);
        log.info(
                "Behandler kafka-aktivtet-melding på aktorId: {} med aktivtetId: {}, version: {}",
                aktivitetData.getAktorId(),
                aktivitetData.getAktivitetId(),
                aktivitetData.getVersion()
        );

        sisteEndringService.behandleAktivitet(aktivitetData);
        if (skallIkkeOppdatereAktivitet(aktivitetData)) {
            return;
        }

        aktivitetDAO.tryLagreAktivitetData(aktivitetData);
        utledOgIndekserAktivitetstatuserForAktoerid(AktorId.of(aktivitetData.getAktorId()));
    }

    public void utledOgLagreAlleAktivitetstatuser() {
        List<String> aktoerider = aktivitetDAO.getDistinctAktorIdsFromAktivitet();

        BatchConsumer<String> consumer = batchConsumer(1000, this::utledOgLagreAktivitetstatuser);

        aktoerider.forEach(consumer);

        consumer.flush();

        log.info("Aktivitetstatuser for {} brukere utledet og lagret i databasen", aktoerider.size());
    }

    private void utledOgLagreAktivitetstatuser(List<String> aktoerider) {
        aktoerider.forEach(aktoerId -> {
            AktoerAktiviteter aktoerAktiviteter = aktivitetDAO.getAktiviteterForAktoerid(AktorId.of(aktoerId));
            AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.konverterTilBrukerOppdatering(aktoerAktiviteter, brukerService);
            Optional.ofNullable(aktivitetBrukerOppdateringer)
                    .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDB(Collections.singletonList(oppdatering)));
        });

    }

    public void utledOgIndekserAktivitetstatuserForAktoerid(AktorId aktoerId) {
        AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerId, brukerService, aktivitetDAO);
        Optional.ofNullable(aktivitetBrukerOppdateringer)
                .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDBogIndekser(Collections.singletonList(oppdatering)));
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

}
