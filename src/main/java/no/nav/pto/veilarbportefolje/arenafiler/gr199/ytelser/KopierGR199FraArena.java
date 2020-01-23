package no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;

import javax.inject.Inject;

import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    @Inject
    private AktivitetService aktivitetService;

    private IndekserYtelserHandler indekserHandler;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler) {
        this.indekserHandler = indekserHandler;
    }

    public void startOppdateringAvYtelser() {
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
