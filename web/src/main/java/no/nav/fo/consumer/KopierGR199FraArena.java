package no.nav.fo.consumer;

import javaslang.control.Try;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import no.nav.fo.service.ArenafilService;
import no.nav.fo.util.CopyStream;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.metrics.aspects.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;

public class KopierGR199FraArena {
    static Logger logger = LoggerFactory.getLogger(KopierGR199FraArena.class);

    @Value("${filmottak.loependeYtelser.sftp.username}")
    String username;

    @Value("${filmottak.loependeYtelser.sftp.password}")
    String password;

    @Value("${filmottak.loependeYtelser.sftp}")
    String server;

    @Value("${cluster.ismasternode}")
    boolean isMaster;

    private IndekserYtelserHandler indekserHandler;
    private ArenafilService arenafilService;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler, ArenafilService arenafilService) {
        this.indekserHandler = indekserHandler;
        this.arenafilService = arenafilService;
    }

    @Timed(name = "GR199.kopierOgIndekser")
    @Scheduled(cron = "${filmottak.loependeYtelser.cron}")
    public void kopierOgIndekser() {
        Supplier<Try<InputStream>> hentfil = () -> arenafilService.hentArenafil(server, username, password);

        timed("GR199.hentfil", hentfil)
                .onFailure(log(logger, "Kunne ikke hente ut fil fra sftpserver"))
                .flatMap(timed("GR199.unmarshall", this::unmarshall))
                .onFailure(log(logger, "Unmarshalling feilet"))
                .andThen(timed("GR199.indekser", indekserHandler::indekser))
                .onFailure(log(logger, "Indeksering feilet"));
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
