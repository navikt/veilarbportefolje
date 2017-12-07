package no.nav.fo.filmottak.ytelser;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.filmottak.FilmottakFileUtils;
import no.nav.fo.loependeytelser.LoependeYtelser;
import no.nav.fo.service.AktivitetService;
import no.nav.metrics.aspects.Timed;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    @Value("${cluster.ismasternode}")
    boolean isMaster;

    @Inject
    private AktivitetService aktivitetService;

    private boolean isRunning = false;

    private IndekserYtelserHandler indekserHandler;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler) {
        this.indekserHandler = indekserHandler;
    }

    public void startOppdateringAvYtelser() {
        log.info("Forsøker å starte oppdatering av ytelser.");
        if(this.isRunning()) {
            log.info("Kunne ikke starte ny oppdatering av ytelser fordi den allerede er midt i en oppdatering.");
            return;
        }
        this.isRunning = true;
        kopierOgIndekser();
    }

    @Timed(name = "GR199.kopierOgIndekser")
    void kopierOgIndekser() {
        Supplier<Try<InputStream>> hentfil = () -> FilmottakFileUtils.lesYtelsesFil(new File(filpath, filnavn));
        log.info("Starter oppdatering av ytelser...");

        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();

        Consumer<Throwable> stopped = (t) -> this.isRunning = false;

        timed("GR199.hentfil", hentfil)
                .onFailure(log(log, "Kunne ikke hente ut fil med ytelser via nfs").andThen(stopped))
                .flatMap(timed("GR199.unmarshall", KopierGR199FraArena::unmarshall))
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet").andThen(stopped))
                .andThen(timed("GR199.lagreYtelser", indekserHandler::lagreYtelser))
                .onFailure(log(log, "Hovedindeksering feilet").andThen(stopped))
                .andThen(() -> {
                    this.isRunning = false;
                    log.info("Oppdatering av ytelser fullført");
                });
    }

    boolean isRunning() {
        return this.isRunning;
    }

    static Try<LoependeYtelser> unmarshall(final InputStream stream) {
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance("no.nav.fo.loependeytelser");
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            LoependeYtelser loependeYtelser = ((JAXBElement<LoependeYtelser>) unmarshaller.unmarshal(stream)).getValue();
            return loependeYtelser;
        });
    }
}
