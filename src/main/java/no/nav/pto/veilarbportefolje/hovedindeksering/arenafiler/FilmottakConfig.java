package no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler;

import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr199.ytelser.IndekserYtelserHandler;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak.TiltakRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilmottakConfig {

    @Bean
    public IndekserYtelserHandler indekserYtelserHandler(PersistentOppdatering persistentOppdatering, HovedindekseringRepository hovedindekseringRepository) {
        return new IndekserYtelserHandler(persistentOppdatering, hovedindekseringRepository);
    }

    @Bean
    public KopierGR199FraArena kopierGR199FraArena(IndekserYtelserHandler indekserYtelserHandler, MetricsClient metricsClient, EnvironmentProperties environmentProperties) {
        return new KopierGR199FraArena(indekserYtelserHandler, metricsClient, environmentProperties);
    }

    @Bean
    public TiltakHandler tiltakHandler(
            TiltakRepository tiltakRepository,
            AktivitetDAO aktivitetDAO,
            EnvironmentProperties environmentProperties,
            MetricsClient metricsClient,
            HovedindekseringRepository hovedindekseringRepository
    ) {
        return new TiltakHandler(tiltakRepository, aktivitetDAO, environmentProperties, metricsClient, hovedindekseringRepository);
    }

    public static class SftpConfig {
        private String url;
        private String username;
        private String password;
        private ArenaFilType arenaFilType;

        public SftpConfig(String url, String username, String password, ArenaFilType arenaFilType) {
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
