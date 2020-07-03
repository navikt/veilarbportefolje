package no.nav.pto.veilarbportefolje.arenafiler;

import io.vavr.control.Try;


import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.MetricsClient;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.IndekserYtelserHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakRepository;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.apache.commons.vfs2.FileObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.bind.UnmarshalException;


@Configuration
public class FilmottakConfig {

    private EnvironmentProperties environmentProperties;

    public FilmottakConfig(EnvironmentProperties environmentProperties) {
         this.environmentProperties = environmentProperties;
    }

    public SftpConfig lopendeAktiviteter() {
        return new SftpConfig(environmentProperties.getArenaPaagaaendeAktiviteterUrl(),
                environmentProperties.getArenaFilmottakSFTPUsername(),
                environmentProperties.getArenaFilmottakSFTPPassword(),
                ArenaFilType.GR_199_TILTAK);
    }

    public SftpConfig lopendeYtelser() {
        return new SftpConfig(
                environmentProperties.getArenaLoependeYtelserUrl(),
                environmentProperties.getArenaFilmottakSFTPUsername(),
                environmentProperties.getArenaFilmottakSFTPPassword(),
                ArenaFilType.GR_202_YTELSER);
    }

    @Bean
    public IndekserYtelserHandler indekserYtelserHandler(BrukerRepository brukerRepository, PersistentOppdatering persistentOppdatering) {
        return new IndekserYtelserHandler(brukerRepository, persistentOppdatering);
    }

    @Bean
    public KopierGR199FraArena kopierGR199FraArena(AktivitetService aktivitetService, IndekserYtelserHandler indekserYtelserHandler, MetricsClient metricsClient) {
        return new KopierGR199FraArena(aktivitetService, indekserYtelserHandler, metricsClient);
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

    public Try<FileObject> hentTiltaksFil() {
        return FilmottakFileUtils.hentFil(lopendeAktiviteter());
    }

    public Try<FileObject> hentYtelseFil() {
        return FilmottakFileUtils.hentFil(lopendeYtelser());
    }


    public HealthCheckResult sftpLopendeYtelserPing(Try<FileObject> hentYtelseFil) {
        FileObject tiltakFil = hentYtelseFil.getOrElseThrow(() -> new RuntimeException());
        Try<TiltakOgAktiviteterForBrukere> tiltak = FilmottakFileUtils.unmarshallTiltakFil(tiltakFil);
        if (tiltak.isFailure()) {
            return innlesingAvFilFeilet(environmentProperties.getArenaLoependeYtelserUrl());
        } else {
            return HealthCheckResult.healthy();
        }
    }


    public HealthCheckResult sftpTiltakPing(Try<FileObject> hentTiltaksFil) {
        FileObject ytelseFil = hentTiltaksFil.getOrElseThrow(() -> new RuntimeException());
        Try<LoependeYtelser> ytelser = FilmottakFileUtils.unmarshallLoependeYtelserFil(ytelseFil);
        if (ytelser.isFailure()) {
            return innlesingAvFilFeilet(environmentProperties.getArenaPaagaaendeAktiviteterUrl());
        }
        return HealthCheckResult.healthy();
    }

    private static HealthCheckResult innlesingAvFilFeilet(String sftpUrl) {
        String message = String.format("Kunne ikke unmarshalle fil: %s", sftpUrl);
        return HealthCheckResult.unhealthy(message, new UnmarshalException(message));
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
