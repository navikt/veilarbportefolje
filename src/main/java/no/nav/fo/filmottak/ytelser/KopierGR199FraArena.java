package no.nav.fo.filmottak.ytelser;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.filmottak.FilmottakFileUtils;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.LockService;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    @Inject
    private AktivitetService aktivitetService;

    @Inject
    private LockService lockService;

    private IndekserYtelserHandler indekserHandler;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler) {
        this.indekserHandler = indekserHandler;
    }

    public void startOppdateringAvYtelser() {
        lockService.runWithLock(this::kopierOgIndekser);
    }

    private void kopierOgIndekser() {
        log.info("Indeksering: Starter oppdatering av ytelser...");
        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();
        timed("GR199.hentfil", () -> FilmottakFileUtils.lesYtelsesFil(new File(filpath, filnavn)))
                .onFailure(log(log, "Kunne ikke hente ut fil med ytelser via nfs"))
                .flatMap(timed("indexering.GR199.unmarshall", KopierGR199FraArena::unmarshall))
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet"))
                .andThen(timed("indexering.GR199.lagreYtelser", indekserHandler::lagreYtelser))
                .onFailure(log(log, "Hovedindeksering feilet"));
        log.info("Indeksering: Fullført oppdatering av ytelser");
    }

    static Try<LoependeYtelser> unmarshall(final InputStream stream) {
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance("no.nav.melding.virksomhet.loependeytelser.v1");
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            return unmarshaller.unmarshal(new StreamSource(stream), LoependeYtelser.class).getValue();
        });
    }
}
