package no.nav.fo.config;

import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.filmottak.ytelser.IndekserYtelserHandler;
import no.nav.fo.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.filmottak.tiltak.TiltakRepository;
import no.nav.fo.service.ArenafilService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArenafilConfig {

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
    public TiltakHandler tiltakHandler(TiltakRepository tiltakRepository) {
        return new TiltakHandler(tiltakRepository);
    }
}
