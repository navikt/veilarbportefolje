package no.nav.fo.service;


import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.util.AktivitetUtils;
import no.nav.fo.util.BatchConsumer;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static no.nav.fo.util.AktivitetUtils.hentAktivitetBrukerOppdatering;
import static no.nav.fo.util.BatchConsumer.batchConsumer;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class AktivitetService {

    @Inject
    private AktoerService aktoerService;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private PersistentOppdatering persistentOppdatering;

    public void tryUtledOgLagreAlleAktivitetstatuser() {
        Try.of(() ->
                timed("aktiviteter.utled.alle.statuser", () -> {
                    utledOgLagreAlleAktivitetstatuser();
                    return null;
                })
        ).onFailure(e -> log.error("Kunne ikke lagre alle aktive statuser: {}", e.getMessage()));
    }

    public void utledOgLagreAlleAktivitetstatuser() {
        List<String> aktoerider = brukerRepository.getDistinctAktoerIdsFromAktivitet();

        BatchConsumer<String> consumer = batchConsumer(1000, this::utledOgLagreAktivitetstatuser);

        utledOgLagreAktivitetstatuser(aktoerider, consumer);
        consumer.flush();

        log.info("Aktivitetstatuser for {} brukere utledet og lagret i databasen", aktoerider.size());
    }

    public void utledOgLagreAktivitetstatuser(List<String> aktoerider, Consumer<String> prosess) {
        aktoerider.forEach(prosess);
    }

    public void utledOgLagreAktivitetstatuser(List<String> aktoerider) {

        timed(
                "aktiviteter.utled.statuser",
                () -> {
                    List<AktoerAktiviteter> aktoerAktiviteter = timed("aktiviteter.hent.for.liste", ()-> brukerRepository.getAktiviteterForListOfAktoerid(aktoerider));
                    List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer = timed("aktiviteter.konverter.til.brukeroppdatering", ()->AktivitetUtils.konverterTilBrukerOppdatering(aktoerAktiviteter, aktoerService));

                    timed("aktiviteter.persistent.lagring", () -> {
                        aktivitetBrukerOppdateringer.forEach( oppdatering -> persistentOppdatering.hentDataOgLagre(oppdatering));
                        return null;
                    });
                    return null;
                    },
                (timer, success) -> timer.addTagToReport("antallAktiviteter", Objects.toString(aktoerider.size()))
        );

    }

    public void utledOgIndekserAktivitetstatuserForAktoerid(String aktoerid) {
        persistentOppdatering.lagre(hentAktivitetBrukerOppdatering(aktoerid, aktoerService, brukerRepository));
    }
}
