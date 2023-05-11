package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.Period;
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

    public List<BarnUnder18AarData> hentBarnUnder18Aar(String fnr) {
        List<BarnUnder18AarData> barn = dbReadOnly.queryForList("""
                            SELECT * FROM BRUKER_DATA_BARN WHERE FORESATT_IDENT = ?
                        """, fnr).stream()
                .map(this::mapTilBarnUnder18)
                .toList();

        return barn;
    }


    public List<Fnr> hentForeldreansvarForPerson(Fnr fnrForesatt) {
        List<Fnr> barn = dbReadOnly.queryForList("""
                            SELECT barn_ident FROM foreldreansvar WHERE FORESATT_IDENT = ?
                        """, String.class, fnrForesatt.get()).stream()
                .map(Fnr::of)
                .toList();

        return barn;
    }

    public Boolean finnesBarnIForeldreansvar(Fnr fnrBarn) {
        Integer numOfRows = dbReadOnly.queryForObject("""
                            SELECT COUNT(*) FROM foreldreansvar WHERE barn_ident = ?
                        """, Integer.class, fnrBarn.get());

        return numOfRows > 0;
    }

    public void lagreBarnData(Fnr barnIdent, LocalDate barnFoedselsdato, String diskresjonskode) {
        db.update("""
                        INSERT INTO bruker_data_barn (barn_ident, barn_foedselsdato, barn_diskresjonkode)
                        VALUES(?,?,?) ON CONFLICT (barn_ident) DO UPDATE SET (barn_foedselsdato, barn_diskresjonkode) = (excluded.barn_foedselsdato, excluded.barn_diskresjonkode)
                         """,
                barnIdent.get(), barnFoedselsdato, diskresjonskode);
    }

    public void lagreForeldreansvar(Fnr foresattIdent, Fnr barnIdent) {
        db.update("""
                        INSERT INTO foreldreansvar (foresatt_ident, barn_ident)
                        VALUES(?,?) ON CONFLICT (foresatt_ident, barn_ident) DO NOTHING 
                         """,
                foresattIdent.get(), barnIdent.get());
    }

    public void slettForeldreansvar(Fnr fnrForesatt, Fnr fnrBarn) {
        db.update("""
                        DELETE FROM foreldreansvar WHERE foresatt_ident = ? AND barn_ident = ?
                         """,
                fnrForesatt.get(), fnrBarn.get());
    }

    public void slettBarnData(Fnr fnrBarn) {
        db.update("""
                        DELETE FROM bruker_data_barn WHERE barn_ident = ?
                         """,
                fnrBarn.get());
    }


    private BarnUnder18AarData mapTilBarnUnder18(Map<String, Object> rs) {
        return new BarnUnder18AarData(
                alderFraFodselsdato(toLocalDateOrNull((java.sql.Date) rs.get("BARN_FOEDSELSDATO")),
                        LocalDate.now()), (boolean) rs.get("BOR_MED_FORESATT"),
                (String) rs.get("BARN_DISKRESJONKODE"));
    }

    public static Long alderFraFodselsdato(LocalDate date, LocalDate now) {
        Integer age = Period.between(date, now).getYears();
        return age.longValue();
    }
}


