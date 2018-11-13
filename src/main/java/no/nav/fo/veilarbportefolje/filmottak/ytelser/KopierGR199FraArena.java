package no.nav.fo.veilarbportefolje.filmottak.ytelser;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils;
import no.nav.fo.veilarbportefolje.service.AktivitetService;

import javax.inject.Inject;

import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.TOTALINDEKSERING;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.TOTALINDEKSERING_LOCK_AT_MOST_UNTIL;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;
import static no.nav.fo.veilarbportefolje.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    @Inject
    private AktivitetService aktivitetService;

    @Inject
    private LockingTaskExecutor lockingTaskExecutor;

    private IndekserYtelserHandler indekserHandler;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler) {
        this.indekserHandler = indekserHandler;
    }

    public void startOppdateringAvYtelser() {
        lockingTaskExecutor.executeWithLock(this::kopierOgIndekser,
                new LockConfiguration(TOTALINDEKSERING, TOTALINDEKSERING_LOCK_AT_MOST_UNTIL));
    }

    private void kopierOgIndekser() {
        log.info("Indeksering: Starter oppdatering av ytelser...");
        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();
        timed("GR199.hentfil", () -> FilmottakFileUtils.hentFil(LOPENDEYTELSER_SFTP))
                .onFailure(log(log, "Kunne ikke hente ut fil med ytelser via nfs"))
                .flatMap(timed("indexering.GR199.unmarshall", FilmottakFileUtils::unmarshallLoependeYtelserFil))
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet"))
                .andThen(timed("indexering.GR199.lagreYtelser", indekserHandler::lagreYtelser))
                .onFailure(log(log, "Hovedindeksering feilet"));
        log.info("Indeksering: Fullført oppdatering av ytelser");
    }
}
