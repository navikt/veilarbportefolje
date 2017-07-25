package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.service.ArenafilService;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.metrics.aspects.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

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
    static Logger logger = LoggerFactory.getLogger(KopierGR199FraArena.class);

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    @Value("${cluster.ismasternode}")
    boolean isMaster;

    @Inject
    SolrService solrService;

    boolean isRunning = false;

    private IndekserYtelserHandler indekserHandler;
    private ArenafilService arenafilService;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler, ArenafilService arenafilService) {
        this.indekserHandler = indekserHandler;
        this.arenafilService = arenafilService;
    }

    @Timed(name = "GR199.kopierOgIndekser")
    @Scheduled(cron = "${filmottak.loependeYtelser.cron}")
    public void kopierOgIndekser() {
        isRunning = true;
        Supplier<Try<InputStream>> hentfil = () -> arenafilService.hentArenafil(new File(filpath, filnavn));

        logger.info("Starter reindeksering...");

        Consumer<Throwable> stopped = (t) -> this.isRunning = false;

        timed("GR199.hentfil", hentfil)
                .onFailure(log(logger, "Kunne ikke hente ut fil via nfs").andThen(stopped))
                .flatMap(timed("GR199.unmarshall", this::unmarshall))
                .onFailure(log(logger, "Unmarshalling feilet").andThen(stopped))
                .andThen(timed("GR199.indekser", indekserHandler::indekser))
                .onFailure(log(logger, "Indeksering feilet").andThen(stopped))
                .andThen(() -> solrService.hovedindeksering())
                .onFailure(log(logger, "Feil under hovedindeksering").andThen(stopped))
                .andThen(() -> {
                    this.isRunning = false;
                    logger.info("Reindeksering ferdig...");
                });
    }

    public boolean isRunning() {
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
