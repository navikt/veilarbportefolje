package no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetService;


import static no.nav.pto.veilarbportefolje.metrikker.MetricsUtils.timed;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    private final AktivitetService aktivitetService;
    private final IndekserYtelserHandler indekserHandler;
    private final MetricsClient metricsClient;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler, AktivitetService aktivitetService, MetricsClient metricsClient) {
        this.indekserHandler = indekserHandler;
        this.aktivitetService = aktivitetService;
        this.metricsClient = metricsClient;
    }

    public void startOppdateringAvYtelser() {
        log.info("Indeksering: Starter oppdatering av ytelser...");
        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser(); //TODO VARFÖR BEHÖVER MAN GÖRA DETTA VID INLÄSNING AV YTELSER?
        timed("portefolje.sftp.tiltak",
        FilmottakFileUtils.hentYtelseFil()
                .onFailure(log(log, "Kunne ikke hente ut fil med ytelser via nfs"))
                .flatMap(FilmottakFileUtils::unmarshallLoependeYtelserFil)
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet"))
                .andThen(indekserHandler::lagreYtelser)
                .onFailure(log(log, "Hovedindeksering feilet"));

        log.info("Indeksering: Fullført oppdatering av ytelser");
    }
}
