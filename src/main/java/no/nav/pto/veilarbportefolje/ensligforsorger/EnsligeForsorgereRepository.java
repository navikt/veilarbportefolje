package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EnsligeForsorgereRepository {
    private final JdbcTemplate db;

    public void lagreDataForEnsligeForsorgere() {

    }

    public void hentDataForEnsligeForsorgere(String personIdent) {
        String sql = """
                SELECT vedtakId, 
                """;


    }


}
