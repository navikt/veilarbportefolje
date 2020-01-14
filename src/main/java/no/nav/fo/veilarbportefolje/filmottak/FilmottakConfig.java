package no.nav.fo.veilarbportefolje.filmottak;

import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakRepository;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.IndekserYtelserHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.util.UUID;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class FilmottakConfig {

    public static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME = "VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME";
    public static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD = "VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD";

    public static final SftpConfig AKTIVITETER_SFTP = new SftpConfig(
            getRequiredProperty("SFTP_GR202_ARENA_PAAGAAENDE_AKTIVITETER_URL"),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD));

    public static final SftpConfig LOPENDEYTELSER_SFTP = new SftpConfig(
            getRequiredProperty("SFTP_GR199_ARENA_LOEPENDE_YTELSER_URL"),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD));

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
    public TiltakHandler tiltakHandler(
            TiltakRepository tiltakRepository,
            AktivitetDAO aktivitetDAO,
            AktoerService aktoerService,
            BrukerRepository brukerRepository) {
        return new TiltakHandler(tiltakRepository, aktivitetDAO, aktoerService, brukerRepository);
    }

    @Bean
    public Pingable sftpLopendeYtelserPing() {
        return sftpPing(LOPENDEYTELSER_SFTP);
    }

    @Bean
    public Pingable sftpTiltakPing() {
        return sftpPing(AKTIVITETER_SFTP);
    }

    private Pingable sftpPing(SftpConfig sftpConfig) {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                sftpConfig.getUrl(),
                "Sjekker henting av fil over sftp",
                true
        );

        return () -> {
            try {
                FileObject fileObject = FilmottakFileUtils.hentFilViaSftp(sftpConfig).get();
                if (fileObject.exists()) {
                    return lyktes(metadata);
                }
                return feilet(metadata, new FileNotFoundException("File not found at " + sftpConfig.getUrl()));
            } catch (FileSystemException e) {
                return feilet(metadata, e);
            }
        };
    }

    public static class SftpConfig {
        private String url;
        private String username;
        private String password;

        SftpConfig(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        public String getUrl() {
            return url;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }
    }
}
