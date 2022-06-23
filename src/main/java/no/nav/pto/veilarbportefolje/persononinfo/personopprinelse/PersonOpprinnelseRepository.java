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

    public List<String> hentFoedeland() {
        return dbReadOnly.queryForList("""
                    SELECT DISTINCT foedeland FROM bruker_data ORDER BY foedeland ASC 
                """, String.class);
    }

    public Set<String> hentTolkSpraak() {
        List<String> talespraakList = dbReadOnly.queryForList("""
                    SELECT DISTINCT talespraaktolk FROM bruker_data 
                """, String.class);

        List<String> tegnspraakList = dbReadOnly.queryForList("""
                    SELECT DISTINCT tegnspraaktolk FROM bruker_data
                """, String.class);

        Set<String> uniqueEntries = new HashSet<>();
        uniqueEntries.addAll(talespraakList);
        uniqueEntries.addAll(tegnspraakList);

        return uniqueEntries;
    }
}
