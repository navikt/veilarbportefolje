package no.nav.fo.veilarbportefolje.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.PersistentOppdatering;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.veilarbportefolje.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.veilarbportefolje.util.AktivitetUtils;
import no.nav.fo.veilarbportefolje.util.BatchConsumer;

import javax.inject.Inject;
import java.util.List;

import static io.vavr.control.Try.run;
import static no.nav.fo.veilarbportefolje.util.BatchConsumer.batchConsumer;

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
        aktivitetDAO.slettAktivitetDatoer();
        run(this::utledOgLagreAlleAktivitetstatuser).onFailure(e -> log.error("Kunne ikke lagre alle aktivitetstatuser", e));
    }

    void utledOgLagreAlleAktivitetstatuser() {
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

    public void utledOgIndekserAktivitetstatuserForAktoerid(List<AktoerId> aktoerIds) {
        List<AktivitetBrukerOppdatering> aktivitetBrukerOppdateringer = AktivitetUtils.hentAktivitetBrukerOppdateringer(aktoerIds,aktoerService,aktivitetDAO);
        persistentOppdatering.lagreBrukeroppdateringerIDBogIndekser(aktivitetBrukerOppdateringer);
    }
}
