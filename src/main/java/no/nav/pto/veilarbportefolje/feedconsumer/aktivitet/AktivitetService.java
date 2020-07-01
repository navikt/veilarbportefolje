package no.nav.pto.veilarbportefolje.feedconsumer.aktivitet;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.vavr.control.Try.run;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

@Slf4j
@Service
public class AktivitetService {

    private final AktoerService aktoerService;
    private final AktivitetDAO aktivitetDAO;
    private final PersistentOppdatering persistentOppdatering;

    @Autowired
    public AktivitetService(AktoerService aktoerService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering) {
        this.aktivitetDAO = aktivitetDAO;
        this.aktoerService = aktoerService;
        this.persistentOppdatering = persistentOppdatering;
    }

    public void oppdaterAktiviteter(List<AktivitetDataFraFeed> data) {
        List<AktivitetDataFraFeed> avtalteAktiviteter = data
                .stream()
                .filter(AktivitetDataFraFeed::isAvtalt)
                .collect(toList());

        avtalteAktiviteter.forEach(this::lagreAktivitetData);

       List<AktoerId> aktoerIds = avtalteAktiviteter
                .stream().map(AktivitetDataFraFeed::getAktorId)
                .distinct()
                .map(AktoerId::of)
                .collect(toList());

       utledOgIndekserAktivitetstatuserForAktoerid(aktoerIds);
    }

    public void tryUtledOgLagreAlleAktivitetstatuser() {
        utledOgLagreAlleAktivitetstatuser(); // TODO VARFÖR KALLAR MAN TVÅ GÅNGER PÅ DENNA FUNKTION??
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

    private void lagreAktivitetData(AktivitetDataFraFeed aktivitet) {
        try {
            if (aktivitet.isHistorisk()) {
                aktivitetDAO.deleteById(aktivitet.getAktivitetId());
            } else {
                aktivitetDAO.upsertAktivitet(aktivitet);
            }
        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata fra feed for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
        }
    }

}
