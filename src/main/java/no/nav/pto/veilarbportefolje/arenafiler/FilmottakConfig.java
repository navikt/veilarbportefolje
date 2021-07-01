package no.nav.pto.veilarbportefolje.arenafiler;

import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.IndekserYtelserHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilmottakConfig {

    @Bean
    public IndekserYtelserHandler indekserYtelserHandler(BrukerRepository brukerRepository, PersistentOppdatering persistentOppdatering) {
        return new IndekserYtelserHandler(persistentOppdatering, brukerRepository);
    }

    @Bean
    public KopierGR199FraArena kopierGR199FraArena(AktivitetService aktivitetService, IndekserYtelserHandler indekserYtelserHandler, MetricsClient metricsClient, EnvironmentProperties environmentProperties) {
        return new KopierGR199FraArena(indekserYtelserHandler, aktivitetService, metricsClient, environmentProperties);
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
