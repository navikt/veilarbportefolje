package no.nav.pto.veilarbportefolje.persononinfo.bosted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BostedRepository {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public List<String> hentBydel(String enhetId) {
        return dbReadOnly.queryForList("""
                            SELECT DISTINCT bydelsnummer FROM bruker_data bd, oppfolgingsbruker_arena_v2 op WHERE bd.freg_ident = op.fodselsnr AND nav_kontor = ?  
                        """, enhetId)
                .stream()
                .filter(rs -> rs.get("bydelsnummer") != null)
                .map(rs -> String.valueOf(rs.get("bydelsnummer")))
                .filter(rs -> !rs.isEmpty())
                .toList();
    }

    public List<String> hentKommune(String enhetId) {
        return dbReadOnly.queryForList("""
                            SELECT DISTINCT kommunenummer FROM bruker_data bd, oppfolgingsbruker_arena_v2 op WHERE bd.freg_ident = op.fodselsnr AND nav_kontor = ?  
                        """, enhetId)
                .stream()
                .filter(rs -> rs.get("kommunenummer") != null)
                .map(rs -> String.valueOf(rs.get("kommunenummer")))
                .filter(rs -> !rs.isEmpty())
                .toList();
    }
}
