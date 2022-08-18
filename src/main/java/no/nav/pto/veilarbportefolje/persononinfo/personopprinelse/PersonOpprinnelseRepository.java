package no.nav.pto.veilarbportefolje.persononinfo.personopprinelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PersonOpprinnelseRepository {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public List<String> hentFoedeland(String enhetId) {
        return dbReadOnly.queryForList("""
                            SELECT DISTINCT foedeland FROM bruker_data bd, oppfolgingsbruker_arena_v2 op WHERE bd.freg_ident = op.fodselsnr AND nav_kontor = ? ORDER BY foedeland ASC 
                        """, enhetId)
                .stream()
                .filter(x -> x.get("foedeland") != null)
                .map(x -> String.valueOf(x.get("foedeland")))
                .filter(x -> !x.isEmpty())
                .toList();
    }

    public Set<String> hentTolkSpraak(String enhetId) {
        List<String> talespraakList = dbReadOnly.queryForList("""
                            SELECT DISTINCT talespraaktolk FROM bruker_data bd, oppfolgingsbruker_arena_v2 op WHERE bd.freg_ident = op.fodselsnr AND nav_kontor = ?
                        """, enhetId)
                .stream()
                .filter(x -> x.get("talespraaktolk") != null)
                .map(x -> String.valueOf(x.get("talespraaktolk")))
                .filter(x -> !x.isEmpty())
                .toList();
        ;

        List<String> tegnspraakList = dbReadOnly.queryForList("""
                            SELECT DISTINCT tegnspraaktolk FROM bruker_data bd, oppfolgingsbruker_arena_v2 op WHERE bd.freg_ident = op.fodselsnr AND nav_kontor = ?
                        """, enhetId)
                .stream()
                .filter(x -> x.get("tegnspraaktolk") != null)
                .map(x -> String.valueOf(x.get("tegnspraaktolk")))
                .filter(x -> !x.isEmpty())
                .toList();
        ;

        Set<String> uniqueEntries = new HashSet<>();
        uniqueEntries.addAll(talespraakList);
        uniqueEntries.addAll(tegnspraakList);

        return uniqueEntries;
    }
}
