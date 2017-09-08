package no.nav.fo.filmottak;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.tiltak.TiltakRepository;
import no.nav.fo.filmottak.ytelser.IndekserYtelserHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.service.AktoerService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileNotFoundException;

@Configuration
public class FilmottakConfig {

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    @Value("${filmottak.tiltak.sftp.URI}")
    private String URI;

    @Value("${veilarbportefolje.filmottak.sftp.login.username}")
    private String filmottakBrukernavn;

    @Value("${veilarbportefolje.filmottak.sftp.login.password}")
    private String filmottakPassord;

    @Value("${environment.name}")
    private String miljo;

    @Bean
    public IndekserYtelserHandler indekserYtelserHandler() {
        return new IndekserYtelserHandler();
    }

    @Bean
    public KopierGR199FraArena kopierGR199FraArena(IndekserYtelserHandler indekserYtelserHandler) {
        return new KopierGR199FraArena(indekserYtelserHandler);
    }

    @Bean
    public TiltakRepository tiltakRepository() {
        return new TiltakRepository();
    }


    @Bean
    public TiltakHandler tiltakHandler(TiltakRepository tiltakRepository, BrukerRepository brukerRepository, AktoerService aktoerService) {
        return new TiltakHandler(tiltakRepository, brukerRepository, aktoerService);
    }

    @Bean
    public Pingable nfsYtelserPing() {
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
            "NFS via" + System.getProperty("loependeytelser.path"),
            "Sjekker connection til fil med ytelser (nfs)",
            true
        );

        return () -> {
            File file = new File(filpath, filnavn);
            if (file.exists()) {
                return Pingable.Ping.lyktes(metadata);
            } else {
                return Pingable.Ping.feilet(metadata, new FileNotFoundException("File not found at " + filpath + filnavn));
            }
        };
    }

    @Bean
    public Pingable sftpTiltakPing() {
        String komplettURI = this.URI.replace("<miljo>", this.miljo).replace("<brukernavn>", this.filmottakBrukernavn).replace("<passord>", filmottakPassord);
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
            this.URI,
            "Sjekker connection til fil med tiltak (sftp)",
            true
        );

        return () -> {
            try {
                FileObject fileObject = FilmottakFileUtils.hentTiltakFil(komplettURI).get();
                if(fileObject.exists()) {
                    return Pingable.Ping.lyktes(metadata);
                }
                return Pingable.Ping.feilet(metadata, new FileNotFoundException("File not found at " + komplettURI));
            } catch (FileSystemException e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }
}
