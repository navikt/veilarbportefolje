package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
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

    @Autowired
    public AktivitetService(AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering, BrukerService brukerService) {
        this.aktivitetDAO = aktivitetDAO;
        this.brukerService = brukerService;
        this.persistentOppdatering = persistentOppdatering;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {

        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);

        if (skallIkkeOppdatereAktivitet(aktivitetData)) {
            return;
        }

        lagreAktivitetData(aktivitetData);

        utledOgIndekserAktivitetstatuserForAktoerid(AktoerId.of(aktivitetData.getAktorId()));
    }

    public void utledOgLagreAlleAktivitetstatuser() {
        List<String> aktoerider = aktivitetDAO.getDistinctAktoerIdsFromAktivitet();

        BatchConsumer<String> consumer = batchConsumer(1000, this::utledOgLagreAktivitetstatuser);

        aktoerider.forEach(consumer);

        consumer.flush();

        log.info("Aktivitetstatuser for {} brukere utledet og lagret i databasen", aktoerider.size());
    }

    private void utledOgLagreAktivitetstatuser(List<String> aktoerider) {
        aktoerider.forEach(aktoerId -> {
            AktoerAktiviteter aktoerAktiviteter = aktivitetDAO.getAktiviteterForAktoerid(AktoerId.of(aktoerId));
            AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.konverterTilBrukerOppdatering(aktoerAktiviteter, brukerService);
            Optional.ofNullable(aktivitetBrukerOppdateringer)
                    .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDB(Collections.singletonList(oppdatering)));
        });

    }

    public void utledOgIndekserAktivitetstatuserForAktoerid(AktoerId aktoerId) {
        AktivitetBrukerOppdatering aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerId, brukerService, aktivitetDAO);
        Optional.ofNullable(aktivitetBrukerOppdateringer)
                .ifPresent(oppdatering -> persistentOppdatering.lagreBrukeroppdateringerIDBogIndekser(Collections.singletonList(oppdatering)));
    }

    private void lagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        try {
            if (aktivitet.isHistorisk()) {
                aktivitetDAO.deleteById(aktivitet.getAktivitetId());
            } else if (aktivitetDAO.erNyVersjonAvAktivitet(aktivitet)) {
                aktivitetDAO.upsertAktivitet(aktivitet);
            }

        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
        }
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
        return !aktivitetData.isAvtalt() || erEnNyOpprettetAktivitet(aktivitetData);
    }

    private boolean erEnNyOpprettetAktivitet(KafkaAktivitetMelding aktivitetData) {
        return aktivitetData.getEndretDato() == null;
    }


}
