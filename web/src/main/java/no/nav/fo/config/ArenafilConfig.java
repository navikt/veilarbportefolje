package no.nav.fo.config;

import no.nav.fo.consumer.IndekserYtelserHandler;
import no.nav.fo.consumer.KopierGR199FraArena;
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
}
