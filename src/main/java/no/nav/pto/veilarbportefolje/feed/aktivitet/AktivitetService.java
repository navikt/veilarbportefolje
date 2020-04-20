package no.nav.pto.veilarbportefolje.feed.aktivitet;


import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.vavr.control.Try.run;
import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

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
        utledOgLagreAlleAktivitetstatuser();
        aktivitetDAO.slettAktivitetDatoer();

        run(this::utledOgLagreAlleAktivitetstatuser)
                .onFailure(e -> log.error("Kunne ikke lagre alle aktivitetstatuser", e));
    }

    public void utledOgLagreAlleAktivitetstatuser() {
        List<String> aktoerider = aktivitetDAO.getDistinctAktoerIdsFromAktivitet();

        BatchConsumer<String> consumer = batchConsumer(1000, this::utledOgLagreAktivitetstatuser);

        aktoerider.forEach(consumer);

        consumer.flush();

        log.info("Aktivitetstatuser for {} brukere utledet og lagret i databasen", aktoerider.size());
    }

    private void utledOgLagreAktivitetstatuser(List<String> aktoerider) {
        List<AktoerAktiviteter> aktoerAktiviteter = aktivitetDAO.getAktiviteterForListOfAktoerid(aktoerider);
        List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer = AktivitetUtils.konverterTilBrukerOppdatering(aktoerAktiviteter, aktoerService);
        persistentOppdatering.lagreBrukeroppdateringerIDB(aktivitetBrukerOppdateringer);
    }

    public CompletableFuture<Void> utledOgIndekserAktivitetstatuserForAktoerid(List<AktoerId> aktoerIds) {
        List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerIds, aktoerService, aktivitetDAO);
        return persistentOppdatering.lagreBrukeroppdateringerIDBogIndekser(aktivitetBrukerOppdateringer);
    }
}
