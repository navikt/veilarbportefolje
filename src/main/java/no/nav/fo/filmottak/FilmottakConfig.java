package no.nav.fo.filmottak;

import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.tiltak.TiltakRepository;
import no.nav.fo.filmottak.ytelser.IndekserYtelserHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.LockService;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.P;
import static no.nav.sbl.util.EnvironmentUtils.getEnvironmentClass;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class FilmottakConfig {

    public static final String VEILARBPORTEFOLJE_SFTP_SELFTEST = "veilarbportefolje.sftp.selftest";

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    private static final String MILJO = getRequiredProperty("environment.name");

    public static final SftpConfig AKTIVITETER_SFTP = new SftpConfig(
            "filmottak",
            "gr202",
            "arena_paagaaende_aktiviteter.xml",
            getRequiredProperty("veilarbportefolje.filmottak.sftp.login.username"),
            getRequiredProperty("veilarbportefolje.filmottak.sftp.login.password"));

    private static final SftpConfig LOPENDEYTELSER_SFTP = new SftpConfig(
            "filmottak-loependeytelser",
            "gr199",
            "arena_loepende_ytelser.xml",
            "srvveilarb-arena", // TODO: Dette er en midlertidig bruker satt opp av Kashmira. Erstatt med Fasit Credential.
            "srvveilarbarena");

    @Inject
    private UnleashService unleashService;

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
    public TiltakHandler tiltakHandler(TiltakRepository tiltakRepository, AktivitetDAO aktivitetDAO, AktoerService aktoerService, BrukerRepository brukerRepository, LockService lockService) {
        return new TiltakHandler(tiltakRepository, aktivitetDAO, aktoerService, brukerRepository, lockService);
    }

    @Bean
    public Pingable nfsYtelserPing() {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "NFS via" + getRequiredProperty("loependeytelser.path"),
                "Sjekker connection til fil med ytelser (nfs)",
                true
        );

        return () -> {
            File file = new File(filpath, filnavn);
            if (file.exists()) {
                return lyktes(metadata);
            } else {
                return feilet(metadata, new FileNotFoundException("File not found at " + filpath + filnavn));
            }
        };
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
        PingMetadata disabledMetadata = new PingMetadata(
                UUID.randomUUID().toString(),
                sftpConfig.getUrl(),
                "Sjekker henting av fil over sftp (disabled by feature-toggle)",
                true
        );

        return () -> {
            boolean enabled = !sftpConfig.equals(LOPENDEYTELSER_SFTP) || unleashService.isEnabled(VEILARBPORTEFOLJE_SFTP_SELFTEST);
            if (!enabled) {
                return lyktes(disabledMetadata);
            }
            try {
                FileObject fileObject = FilmottakFileUtils.hentFil(sftpConfig).get();
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

        SftpConfig(String host, String folder, String filename, String username, String password) {
            this.url = buildUrl(host, folder, filename);
            this.username = username;
            this.password = password;
        }

        private String buildUrl(String host, String folder, String filename) {
            boolean preprod = getEnvironmentClass() != P;
            UriBuilder uriBuilder = UriBuilder.fromPath(folder)
                    .scheme("sftp")
                    .host(host + "." + (preprod ? "preprod.local" : "adeo.no"));
            if (preprod) {
                uriBuilder.path(MILJO);
            }
            return uriBuilder
                    .path(filename)
                    .build()
                    .toString();
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
