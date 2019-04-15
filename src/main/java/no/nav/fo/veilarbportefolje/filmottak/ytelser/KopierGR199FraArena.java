package no.nav.fo.veilarbportefolje.filmottak.ytelser;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils;
import no.nav.fo.veilarbportefolje.service.AktivitetService;

import javax.inject.Inject;

import java.time.Instant;

import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.TOTALINDEKSERING;
import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
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
                new LockConfiguration(TOTALINDEKSERING, Instant.now().plusSeconds(60 * 60 * 3)));
    }

    private void kopierOgIndekser() {
        log.info("Indeksering: Starter oppdatering av ytelser...");
        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();
        FilmottakFileUtils.hentFil(LOPENDEYTELSER_SFTP)
                .onFailure(log(log, "Kunne ikke hente ut fil med ytelser via nfs"))
                .flatMap(FilmottakFileUtils::unmarshallLoependeYtelserFil)
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet"))
                .andThen(indekserHandler::lagreYtelser)
                .onFailure(log(log, "Hovedindeksering feilet"));
        log.info("Indeksering: Fullført oppdatering av ytelser");
    }
}
