package no.nav.fo.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.util.AktivitetUtils;
import no.nav.fo.util.BatchConsumer;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

import static io.vavr.control.Try.run;
import static no.nav.fo.util.BatchConsumer.batchConsumer;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class AktivitetService {

    private AktoerService aktoerService;
    private AktivitetDAO aktivitetDAO;
    private PersistentOppdatering persistentOppdatering;

    @Inject
    public AktivitetService(AktoerService aktoerService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering) {
        this.aktivitetDAO = aktivitetDAO;
        this.aktoerService = aktoerService;
        this.persistentOppdatering = persistentOppdatering;
    }

    public void tryUtledOgLagreAlleAktivitetstatuser() {
        aktivitetDAO.slettutlopsdatoForAktivitet();
        run(
                () -> timed("aktiviteter.utled.alle.statuser", this::utledOgLagreAlleAktivitetstatuser)
        ).onFailure(e -> log.error("Kunne ikke lagre alle aktivitetstatuser", e));
    }

    void utledOgLagreAlleAktivitetstatuser() {
        List<String> aktoerider = aktivitetDAO.getDistinctAktoerIdsFromAktivitet();

        BatchConsumer<String> consumer = batchConsumer(1000, this::utledOgLagreAktivitetstatuser);

        aktoerider.forEach(consumer);

        consumer.flush();

        log.info("Aktivitetstatuser for {} brukere utledet og lagret i databasen", aktoerider.size());
    }

    private void utledOgLagreAktivitetstatuser(List<String> aktoerider) {

        timed(
                "aktiviteter.utled.statuser",
                () -> {
                    List<AktoerAktiviteter> aktoerAktiviteter = timed("aktiviteter.hent.for.liste", () -> aktivitetDAO.getAktiviteterForListOfAktoerid(aktoerider));
                    List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer =
                            timed("aktiviteter.konverter.til.brukeroppdatering", () -> AktivitetUtils.konverterTilBrukerOppdatering(aktoerAktiviteter, aktoerService));

                    timed("aktiviteter.persistent.lagring", () -> persistentOppdatering.lagreBrukeroppdateringerIDB(aktivitetBrukerOppdateringer));
                },
                (timer, success) -> timer.addTagToReport("antallAktiviteter", Objects.toString(aktoerider.size()))
        );

    }

    public void utledOgIndekserAktivitetstatuserForAktoerid(List<AktoerId> aktoerIds) {
        List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerIds,aktoerService,aktivitetDAO);
        persistentOppdatering.lagreBrukeroppdateringerIDBogIndekser(aktivitetBrukerOppdateringer);
    }
}
