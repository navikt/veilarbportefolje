package no.nav.fo.filmottak;

import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.IndekserYtelserHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.filmottak.tiltak.TiltakRepository;
import no.nav.fo.service.ArenafilService;
import no.nav.sbl.dialogarena.types.Pingable;
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


    @Bean
    public IndekserYtelserHandler indekserYtelserHandler() {
        return new IndekserYtelserHandler();
    }

    @Bean
    public ArenafilService arenafilService() {
        return new ArenafilService() {}; // Bruker default impl
    }


    @Bean
    public KopierGR199FraArena kopierGR199FraArena(IndekserYtelserHandler indekserYtelserHandler, ArenafilService arenafilService) {
        return new KopierGR199FraArena(indekserYtelserHandler, arenafilService);
    }

    @Bean
    public TiltakRepository tiltakRepository() {
        return new TiltakRepository();
    }


    @Bean
    public TiltakHandler tiltakHandler(TiltakRepository tiltakRepository) {
        return new TiltakHandler(tiltakRepository);
    }

    @Bean
    public Pingable nfsPing() {
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
            "NFS via" + System.getProperty("loependeytelser.path"),
            "Sjekk om fil med brukere som mottar ytelser ligger på disk",
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
}
