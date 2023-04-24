package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BarnUnder18AarRepository {

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    private final JdbcTemplate db;

    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    List<BarnUnder18AarData> data = new ArrayList<>();

    public List<BarnUnder18AarData> hentBarnUnder18Aar(String fnr) {
        List<BarnUnder18AarData> barn = dbReadOnly.queryForList("""
                            SELECT * FROM BRUKER_DATA_BARN WHERE FORESATT_IDENT = ?
                        """, fnr).stream()
                .map(this::mapTilBarnUnder18)
                .toList();

        return barn;
    }

    public void upsert(Fnr fnr, Boolean borMedForesatt, LocalDate barnFoedselsdato, String diskresjonskode){
          db.update("""
                        INSERT INTO bruker_data_barn (id, foresatt_ident , bor_med_foresatt, barn_foedselsdato, barn_diskresjonkode)
                        VALUES(?,?,?,?,?) ON CONFLICT (id) DO UPDATE SET
                        (id, foresatt_ident, bor_med_foresatt, barn_foedselsdato , barn_diskresjonkode) =
                        (excluded.foresatt_ident, excluded.bor_med_foresatt, excluded.barn_foedselsdato, excluded.barn_diskresjonkode)
                        """,
                fnr.get(), borMedForesatt, barnFoedselsdato, diskresjonskode);
    }




    private BarnUnder18AarData mapTilBarnUnder18(Map<String, Object> rs) {
        return new BarnUnder18AarData((Long) alderFraFDato(toLocalDateOrNull((java.sql.Date) rs.get("BARN_FOEDSELSDATO"))), (boolean) rs.get("BOR_MED_FORESATT"), (String) rs.get("BARN_DISKRESJONKODE"));
    }

    public static Long alderFraFDato(LocalDate date) {
        return 15L;
    }
}
