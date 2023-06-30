package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.alderFraFodselsdato;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BarnUnder18AarRepository {

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    private final JdbcTemplate db;

    public List<Fnr> hentForeldreansvarForPerson(Fnr fnrForesatt) {
        try {
            return dbReadOnly.queryForList("""
                    SELECT barn_ident FROM foreldreansvar WHERE FORESATT_IDENT = ?
                    """, String.class, fnrForesatt.get()).stream().map(Fnr::of).toList();
        } catch (Exception e) {
            log.error("Can't fetch foreldre ansvar " + e, e);
            return List.of();
        }
    }

    public List<Fnr> hentForeldreTilBarn(Fnr fnrBarn) {
        try {
            return dbReadOnly.queryForList("""
                    SELECT FORESATT_IDENT FROM foreldreansvar WHERE BARN_IDENT = ?
                    """, String.class, fnrBarn.get()).stream().map(Fnr::of).toList();
        } catch (Exception e) {
            log.error("Can't fetch foreldre ansvar " + e, e);
            return List.of();
        }
    }

    public List<Fnr> hentAlleBarnOver18() {
        return dbReadOnly.queryForList("""
                    SELECT barn_ident  FROM bruker_data_barn WHERE BARN_FOEDSELSDATO <= NOW() - INTERVAL '18 YEARS';
                """, String.class).stream().map(Fnr::of).toList();
    }

    public Boolean finnesBarnIForeldreansvar(Fnr fnrBarn) {
        Integer numOfRows = dbReadOnly.queryForObject("""
                    SELECT COUNT(*) FROM foreldreansvar WHERE barn_ident = ?
                """, Integer.class, fnrBarn.get());

        return numOfRows > 0;
    }

    public Boolean finnesBarnIForeldreansvar(List<Fnr> fnrBarn) {
        String fnrsparam = fnrBarn.stream().map(Fnr::get).collect(Collectors.joining(",", "{", "}"));
        Integer numOfRows = dbReadOnly.queryForObject("""
                    SELECT COUNT(*) FROM foreldreansvar WHERE barn_ident = any (?::varchar[])
                """, Integer.class, fnrsparam);


        return numOfRows > 0;
    }

    public void lagreBarnData(Fnr barnIdent, LocalDate barnFoedselsdato, String diskresjonskode) {
        try {
            db.update("""
                    INSERT INTO bruker_data_barn (barn_ident, barn_foedselsdato, barn_diskresjonkode)
                    VALUES(?,?,?) ON CONFLICT (barn_ident) DO UPDATE SET (barn_foedselsdato, barn_diskresjonkode) = (excluded.barn_foedselsdato, excluded.barn_diskresjonkode)
                     """, barnIdent.get(), barnFoedselsdato, diskresjonskode);
        } catch (Exception e) {
            log.error("Can't update barn data " + e, e);
        }

    }

    public void oppdatereBarnIdent(Fnr nyIdentBarn, List<Fnr> inaktiveIdenterBarn) {
        if (inaktiveIdenterBarn.isEmpty()) {
            return;
        }
        String inaktiveIdenterBarnStr = inaktiveIdenterBarn.stream().map(Fnr::get).collect(Collectors.joining(",", "{", "}"));
        db.update("""
                UPDATE bruker_data_barn SET barn_ident = ? WHERE barn_ident = ANY (?::varchar[])
                 """, nyIdentBarn.get(), inaktiveIdenterBarnStr);
    }

    public void lagreForeldreansvar(Fnr foresattIdent, Fnr barnIdent) {
        db.update("""
                INSERT INTO foreldreansvar (foresatt_ident, barn_ident)
                VALUES(?,?) ON CONFLICT (foresatt_ident, barn_ident) DO NOTHING 
                 """, foresattIdent.get(), barnIdent.get());
    }

    public boolean slettForeldreansvar(Fnr fnrForesatt, Fnr fnrBarn) {
        int affectedRows = db.update("""
                DELETE FROM foreldreansvar WHERE foresatt_ident = ? AND barn_ident = ?
                 """, fnrForesatt.get(), fnrBarn.get());

        return affectedRows > 0;
    }

    public void slettForeldreansvar(Fnr fnrBarn) {
        db.update("""
                DELETE FROM foreldreansvar WHERE barn_ident = ?
                 """, fnrBarn.get());
    }

    public void slettBarnData(Fnr fnrBarn) {
        db.update("""
                DELETE FROM bruker_data_barn WHERE barn_ident = ?
                 """, fnrBarn.get());
    }

    public BarnUnder18AarData hentInfoOmBarn(Fnr fnrBarn) {
        try {
            return queryForObjectOrNull(() -> dbReadOnly.queryForObject("SELECT * FROM bruker_data_barn WHERE barn_ident = ?", (rs, row) -> new BarnUnder18AarData(alderFraFodselsdato(toLocalDateOrNull(rs.getDate("BARN_FOEDSELSDATO"))), rs.getString("BARN_DISKRESJONKODE")), fnrBarn.get()));
        } catch (Exception e) {
            log.error("Can't get info about barn under 18 " + e, e);
            return null;
        }
    }
}


