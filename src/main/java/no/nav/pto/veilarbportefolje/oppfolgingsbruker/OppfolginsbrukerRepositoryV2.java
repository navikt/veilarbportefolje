package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;


@Slf4j
public class OppfolginsbrukerRepositoryV2 {
    private final JdbcTemplate db;

    public OppfolginsbrukerRepositoryV2(@Qualifier("Postgres") JdbcTemplate db) {
        this.db = db;
    }

    public void LeggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerKafkaDTO oppfolgingsbruker) {
        // TODO: Legg til oppfolgingsbruker i DB
        log.info("LeggTilEllerEndreOppfolginsbruker");
    }

}
