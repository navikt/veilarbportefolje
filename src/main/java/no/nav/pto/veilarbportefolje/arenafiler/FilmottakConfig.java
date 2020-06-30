package no.nav.pto.veilarbportefolje.arenafiler;

import io.vavr.control.Try;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.IndekserYtelserHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakRepository;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import org.apache.commons.vfs2.FileObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.bind.UnmarshalException;
import java.util.UUID;


@Configuration
public class FilmottakConfig {

    public static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME = "VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME";
    public static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD = "VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD";

    public static final SftpConfig AKTIVITETER_SFTP = new SftpConfig(
            getRequiredProperty("SFTP_GR202_ARENA_PAAGAAENDE_AKTIVITETER_URL"),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD),
            ArenaFilType.GR_199_TILTAK

    );

    public static final SftpConfig LOPENDEYTELSER_SFTP = new SftpConfig(
            getRequiredProperty("SFTP_GR199_ARENA_LOEPENDE_YTELSER_URL"),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME),
            getRequiredProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD),
            ArenaFilType.GR_202_YTELSER
    );

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

            switch (sftpConfig.arenaFilType) {
                case GR_199_TILTAK:
                    FileObject tiltakFil = FilmottakFileUtils.hentTiltaksFil().getOrElseThrow(() -> new RuntimeException());
                    Try<TiltakOgAktiviteterForBrukere> tiltak = FilmottakFileUtils.unmarshallTiltakFil(tiltakFil);
                    if (tiltak.isFailure()) {
                        return innlesingAvFilFeilet(sftpConfig, metadata);
                    }
                    break;
                case GR_202_YTELSER:
                    FileObject ytelseFil = FilmottakFileUtils.hentYtelseFil().getOrElseThrow(() -> new RuntimeException());
                    Try<LoependeYtelser> ytelser = FilmottakFileUtils.unmarshallLoependeYtelserFil(ytelseFil);
                    if (ytelser.isFailure()) {
                        return innlesingAvFilFeilet(sftpConfig, metadata);
                    }
                    break;
                default:
                    return feilet(metadata, new IllegalStateException(sftpConfig.getUrl()));
            }

            return lyktes(metadata);
        };
    }

    private Pingable.Ping innlesingAvFilFeilet(SftpConfig sftpConfig, PingMetadata metadata) {
        String message = String.format("Kunne ikke unmarshalle fil: %s", sftpConfig.url);
        return feilet(metadata, new UnmarshalException(message));
    }

    public static class SftpConfig {
        private String url;
        private String username;
        private String password;
        private ArenaFilType arenaFilType;

        SftpConfig(String url, String username, String password, ArenaFilType arenaFilType) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.arenaFilType = arenaFilType;
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

        public ArenaFilType getArenaFilType() {
            return arenaFilType;
        }
    }
}
