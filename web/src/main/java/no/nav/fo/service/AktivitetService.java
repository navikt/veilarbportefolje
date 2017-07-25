package no.nav.fo.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.Aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.Aktivitet.AktoerAktiviteter;
import no.nav.fo.util.AktivitetUtils;
import no.nav.fo.util.BatchConsumer;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

import static no.nav.fo.util.AktivitetUtils.hentAktivitetBrukerOppdatering;
import static no.nav.fo.util.BatchConsumer.batchConsumer;

@Slf4j
public class AktivitetService {

    @Inject
    private AktoerService aktoerService;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private PersistentOppdatering persistentOppdatering;


    public void utledOgLagreAlleAktivitetstatuser() {
        List<String> aktoerider = brukerRepository.getDistinctAktoerIdsFromAktivitet();

        BatchConsumer<String> consumer = batchConsumer(10000, this::utledOgLagreAktivitetstatuser);

        utledOgLagreAktivitetstatuser(aktoerider, consumer);
        consumer.flush();

        log.info("Aktivitetstatuser for {} brukere utledet og lagret i databasen", aktoerider.size());
    }

    public void utledOgLagreAktivitetstatuser(List<String> aktoerider, Consumer<String> prosess) {
        aktoerider.forEach(prosess);
    }

    public void utledOgLagreAktivitetstatuser(List<String> aktoerider) {

        List<AktoerAktiviteter> aktoerAktiviteter = brukerRepository.getAktiviteterForListOfAktoerid(aktoerider);
        List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer = AktivitetUtils.konverterTilBrukerOppdatering(aktoerAktiviteter, aktoerService);

        aktivitetBrukerOppdateringer.forEach( oppdatering -> persistentOppdatering.hentDataOgLagre(oppdatering));
    }

    public void utledOgIndekserAktivitetstatuserForAktoerid(String aktoerid) {
        persistentOppdatering.lagre(hentAktivitetBrukerOppdatering(aktoerid, aktoerService, brukerRepository));
    }
}
