package no.nav.fo.filmottak.ytelser;

import io.vavr.control.Try;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.filmottak.FilLeser;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.metrics.aspects.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class KopierGR199FraArena {
    private static Logger logger = LoggerFactory.getLogger(KopierGR199FraArena.class);

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
    private FilLeser filLeser;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler, FilLeser filLeser) {
        this.indekserHandler = indekserHandler;
        this.filLeser = filLeser;
    }

    public void startOppdateringAvYtelser() {
        if(this.isRunning()) {
            logger.info("Kunne ikke starte ny oppdatering av ytelser fordi den allerede er midt i en oppdatering");
            return;
        }
        this.isRunning = true;
        kopierOgIndekser();
    }

    @Timed(name = "GR199.kopierOgIndekser")
    void kopierOgIndekser() {
        Supplier<Try<InputStream>> hentfil = () -> filLeser.lesFil(new File(filpath, filnavn));
        logger.info("Starter oppdatering av ytelser...");

        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser();

        Consumer<Throwable> stopped = (t) -> this.isRunning = false;

        timed("GR199.hentfil", hentfil)
                .onFailure(log(logger, "Kunne ikke hente ut fil via nfs").andThen(stopped))
                .flatMap(timed("GR199.unmarshall", this::unmarshall))
                .onFailure(log(logger, "Unmarshalling feilet").andThen(stopped))
                .andThen(timed("GR199.indekser", indekserHandler::indekser))
                .onFailure(log(logger, "Indeksering feilet").andThen(stopped))
                .andThen(() -> {
                    this.isRunning = false;
                    logger.info("Oppdatering av ytelser ferdig...");
                });
    }

    boolean isRunning() {
        return this.isRunning;
    }

    private Try<LoependeYtelser> unmarshall(final InputStream stream) {
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance("no.nav.melding.virksomhet.loependeytelser.v1");
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            LoependeYtelser loependeYtelser = ((JAXBElement<LoependeYtelser>) unmarshaller.unmarshal(stream)).getValue();
            return loependeYtelser;
        });
    }
}
