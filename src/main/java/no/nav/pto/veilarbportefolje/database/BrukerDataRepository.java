package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BrukerDataRepository {
    private final JdbcTemplate db;


}
