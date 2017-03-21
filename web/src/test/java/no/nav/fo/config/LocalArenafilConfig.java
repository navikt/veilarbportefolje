package no.nav.fo.config;

import javaslang.control.Try;
import no.nav.fo.consumer.IndekserYtelserHandler;
import no.nav.fo.consumer.KopierGR199FraArena;
import no.nav.fo.service.ArenafilService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class LocalArenafilConfig {

    @Bean
    public IndekserYtelserHandler indekserYtelserHandler() {
        return new IndekserYtelserHandler();
    }

    @Bean
    public ArenafilService arenafilService() throws Exception {
        return new ArenafilService() {
            final byte[] bytes = Files.readAllBytes(Paths.get(
                    ArenafilService.class.getClassLoader().getResource("arena_loepende_ytelser.xml").toURI()
            ));

            @Override
            public Try<InputStream> hentArenafil(String server, String username, String password) {
                return Try.of(() -> new ByteArrayInputStream(bytes));
            }
        };
    }

    @Bean
    public KopierGR199FraArena kopierGR199FraArena(IndekserYtelserHandler indekserYtelserHandler, ArenafilService arenafilService) {
        return new KopierGR199FraArena(indekserYtelserHandler, arenafilService);
    }
}
