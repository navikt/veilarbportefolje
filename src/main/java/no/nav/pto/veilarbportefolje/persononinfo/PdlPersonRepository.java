package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_STATSBORGERSKAP;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PdlPersonRepository {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public void upsertPerson(Fnr fnr, PDLPerson personData) {
        db.update("""
                        INSERT INTO bruker_data (freg_ident, fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedselsdato, 
                        foedeland,  innflyttingTilNorgeFraLand, angittFlyttedato, talespraaktolk, tegnspraaktolk, tolkBehovSistOppdatert,
                        kommunenummer, bydelsnummer, utenlandskAdresse, bostedSistOppdatert)
                        values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        on conflict (freg_ident)
                        do update set (fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedselsdato, 
                        foedeland, innflyttingTilNorgeFraLand, angittFlyttedato, talespraaktolk, tegnspraaktolk, tolkBehovSistOppdatert,
                        kommunenummer, bydelsnummer, utenlandskAdresse, bostedSistOppdatert) =
                        (excluded.fornavn, excluded.etternavn, excluded.mellomnavn, excluded.kjoenn, excluded.er_doed, excluded.foedselsdato, 
                        excluded.foedeland, excluded.innflyttingTilNorgeFraLand, excluded.angittFlyttedato,
                        excluded.talespraaktolk, excluded.tegnspraaktolk, excluded.tolkBehovSistOppdatert,
                        excluded.kommunenummer, excluded.bydelsnummer, excluded.utenlandskAdresse, excluded.bostedSistOppdatert)
                        """,
                fnr.get(), personData.getFornavn(), personData.getEtternavn(), personData.getMellomnavn(),
                personData.getKjonn().name(), personData.isErDoed(), personData.getFoedsel(), personData.getFoedeland(),
                personData.getInnflyttingTilNorgeFraLand(), personData.getAngittFlyttedato(), personData.getTalespraaktolk(),
                personData.getTegnspraaktolk(), personData.getTolkBehovSistOppdatert(),
                personData.getKommunenummer(), personData.getBydelsnummer(), personData.getUtenlandskAdresse(),
                personData.getBostedSistOppdatert());

        updateStatsborgerskap(fnr, personData.getStatsborgerskap());
    }

    private void updateStatsborgerskap(Fnr fnr, List<Statsborgerskap> statsborgerskaps) {
        deleteStatsborgerskapData(fnr);

        if (statsborgerskaps != null) {
            statsborgerskaps.forEach(statsborgerskap -> db.update("""
                    INSERT INTO BRUKER_STATSBORGERSKAP(FREG_IDENT, STATSBORGERSKAP, GYLDIG_FRA, GYLDIG_TIL) VALUES (?, ?, ?, ?)
                    """, fnr.get(), statsborgerskap.getStatsborgerskap(), statsborgerskap.getGyldigFra(), statsborgerskap.getGyldigTil()));
        }
    }

    private void deleteStatsborgerskapData(Fnr fnr) {
        db.update("DELETE FROM BRUKER_STATSBORGERSKAP WHERE FREG_IDENT = ?", fnr.get());
    }

    public void slettLagretBrukerData(List<Fnr> identer) {
        if (identer.isEmpty()) {
            return;
        }
        String identerParam = identer.stream().map(Fnr::get).collect(Collectors.joining(",", "{", "}"));
        db.update("DELETE from bruker_data where freg_ident = any (?::varchar[])", identerParam);
        db.update("DELETE FROM BRUKER_STATSBORGERSKAP WHERE FREG_IDENT = any (?::varchar[]) ", identerParam);
    }

    @SneakyThrows
    public PDLPerson hentPerson(Fnr hentAktivFnr) {
        List<Statsborgerskap> statsborgerskaps = hentGyldigeStatsborgerskapData(hentAktivFnr);
        return queryForObjectOrNull(() ->
                dbReadOnly.queryForObject("select * from bruker_data where freg_ident = ?",
                        (rs, row) -> new PDLPerson()
                                .setFornavn(rs.getString("fornavn"))
                                .setEtternavn(rs.getString("etternavn"))
                                .setMellomnavn(rs.getString("mellomnavn"))
                                .setKjonn(Kjonn.valueOf(rs.getString("kjoenn")))
                                .setErDoed(rs.getBoolean("er_doed"))
                                .setFoedsel(rs.getDate("foedselsdato").toLocalDate())
                                .setFoedeland(rs.getString("foedeland"))
                                .setInnflyttingTilNorgeFraLand(rs.getString("innflyttingTilNorgeFraLand"))
                                .setAngittFlyttedato(toLocalDateOrNull(rs.getString("angittFlyttedato")))
                                .setTalespraaktolk(rs.getString("talespraaktolk"))
                                .setTegnspraaktolk(rs.getString("tegnspraaktolk"))
                                .setTolkBehovSistOppdatert(toLocalDateOrNull(rs.getString("tolkBehovSistOppdatert")))
                                .setStatsborgerskap(statsborgerskaps)
                                .setBydelsnummer(rs.getString("bydelsnummer"))
                                .setKommunenummer(rs.getString("kommunenummer"))
                                .setUtenlandskAdresse(rs.getString("utenlandskAdresse"))
                                .setBostedSistOppdatert(toLocalDateOrNull(rs.getString("bostedSistOppdatert")))
                        , hentAktivFnr.get()));
    }

    private List<Statsborgerskap> hentGyldigeStatsborgerskapData(Fnr fnr) {
        return dbReadOnly.query("""
                    SELECT STATSBORGERSKAP, GYLDIG_FRA, GYLDIG_TIL FROM BRUKER_STATSBORGERSKAP WHERE FREG_IDENT = ?
                """, (rs, rowNum) -> new Statsborgerskap(
                rs.getString("STATSBORGERSKAP"),
                toLocalDateOrNull(rs.getString("GYLDIG_FRA")),
                toLocalDateOrNull(rs.getString("GYLDIG_TIL"))
        ), fnr.get());
    }

    public Map<Fnr, List<Statsborgerskap>> hentGyldigeStatsborgerskapData(List<Fnr> fnrs) {
        String fnrsStr = fnrs.stream().map(Fnr::get).collect(Collectors.joining(",", "{", "}"));

        String sql = """
                SELECT FREG_IDENT, STATSBORGERSKAP, GYLDIG_FRA, GYLDIG_TIL  FROM BRUKER_STATSBORGERSKAP WHERE (GYLDIG_TIL IS NULL OR GYLDIG_TIL > NOW()) AND FREG_IDENT = ANY (?::varchar[])
                """;
        return dbReadOnly.query(sql,
                ps -> ps.setString(1, fnrsStr),
                (ResultSet rs) -> {
                    HashMap<Fnr, List<Statsborgerskap>> results = new HashMap<>();
                    while (rs.next()) {
                        Fnr fnr = Fnr.of(rs.getString(BRUKER_STATSBORGERSKAP.FNR));
                        List<Statsborgerskap> statsborgerskapForBruker = results.getOrDefault(fnr, new ArrayList<>());
                        statsborgerskapForBruker.add(new Statsborgerskap(
                                rs.getString(BRUKER_STATSBORGERSKAP.STATSBORGERSKAP),
                                toLocalDateOrNull(rs.getString(BRUKER_STATSBORGERSKAP.GYLDIG_FRA)),
                                toLocalDateOrNull(rs.getString(BRUKER_STATSBORGERSKAP.GYLDIG_TIL))));
                        results.put(fnr, statsborgerskapForBruker);
                    }
                    return results;
                });
    }
}
