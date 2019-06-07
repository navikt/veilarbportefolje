package no.nav.fo.veilarbportefolje.filmottak.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.FilmottakFileUtils;
import no.nav.fo.veilarbportefolje.service.AktivitetService;

import javax.inject.Inject;

import static no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.LOPENDEYTELSER_SFTP;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;
import static no.nav.fo.veilarbportefolje.util.StreamUtils.log;

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
                .flatMap(timed("indexering.GR199.unmarshall", FilmottakFileUtils::unmarshallLoependeYtelserFil))
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet"))
                .andThen(timed("indexering.GR199.lagreYtelser", indekserHandler::lagreYtelser))
                .onFailure(log(log, "Hovedindeksering feilet"));

        log.info("Indeksering: Fullført oppdatering av ytelser");
    }
}
