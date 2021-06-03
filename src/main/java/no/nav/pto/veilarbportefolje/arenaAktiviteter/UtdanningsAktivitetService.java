package no.nav.pto.veilarbportefolje.arenaAktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.*;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
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
@RequiredArgsConstructor
public class UtdanningsAktivitetService implements KafkaConsumerService<String> {

    private final BrukerService brukerService;
    private final AktivitetDAO aktivitetDAO;
    private final PersistentOppdatering persistentOppdatering;
    private final AtomicBoolean rewind = new AtomicBoolean();

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        KafkaAktivitetMelding aktivitetData = fromJson(kafkaMelding, KafkaAktivitetMelding.class);
        log.info(
                "Behandler utdannings-aktivtet-melding på aktorId: {} med id: {}",
                aktivitetData.getAktorId(),
                aktivitetData.getAktivitetId()
        );

        aktivitetDAO.tryLagreAktivitetData(aktivitetData);
        utledOgIndekserAktivitetstatuserForAktoerid(AktorId.of(aktivitetData.getAktorId()));
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

}
