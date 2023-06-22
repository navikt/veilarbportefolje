package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_STATSBORGERSKAP;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.domene.Sikkerhetstiltak;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
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
        String sikkerhetstiltak_type = personData.getSikkerhetstiltak() != null ? personData.getSikkerhetstiltak().getTiltakstype() : null;
        String sikkerhetstiltak_beskrivelse = personData.getSikkerhetstiltak() != null ? personData.getSikkerhetstiltak().getBeskrivelse() : null;
        LocalDate sikkerhetstiltak_gyldigfra = personData.getSikkerhetstiltak() != null ? personData.getSikkerhetstiltak().getGyldigFra() : null;
        LocalDate sikkerhetstiltak_gyldigtil = personData.getSikkerhetstiltak() != null ? personData.getSikkerhetstiltak().getGyldigTil() : null;

        db.update("""
                        INSERT INTO bruker_data (freg_ident, fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedselsdato,
                        foedeland, talespraaktolk, tegnspraaktolk, tolkBehovSistOppdatert,
                        kommunenummer, bydelsnummer, utenlandskAdresse, bostedSistOppdatert, harUkjentBosted, diskresjonkode,
                        sikkerhetstiltak_type, sikkerhetstiltak_beskrivelse, sikkerhetstiltak_gyldigfra,
                        sikkerhetstiltak_gyldigtil)
                        values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        on conflict (freg_ident)
                        do update set (fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedselsdato,
                        foedeland, talespraaktolk, tegnspraaktolk, tolkBehovSistOppdatert,
                        kommunenummer, bydelsnummer, utenlandskAdresse, bostedSistOppdatert, harUkjentBosted, diskresjonkode,
                        sikkerhetstiltak_type, sikkerhetstiltak_beskrivelse, sikkerhetstiltak_gyldigfra,
                        sikkerhetstiltak_gyldigtil
                        ) =
                        (excluded.fornavn, excluded.etternavn, excluded.mellomnavn, excluded.kjoenn, excluded.er_doed, excluded.foedselsdato,
                        excluded.foedeland,
                        excluded.talespraaktolk, excluded.tegnspraaktolk, excluded.tolkBehovSistOppdatert,
                        excluded.kommunenummer, excluded.bydelsnummer, excluded.utenlandskAdresse, excluded.bostedSistOppdatert, excluded.harUkjentBosted, excluded.diskresjonkode,
                        excluded.sikkerhetstiltak_type, excluded.sikkerhetstiltak_beskrivelse, excluded.sikkerhetstiltak_gyldigfra,
                        excluded.sikkerhetstiltak_gyldigtil)
                        """,
                fnr.get(), personData.getFornavn(), personData.getEtternavn(), personData.getMellomnavn(),
                personData.getKjonn().name(), personData.isErDoed(), personData.getFoedsel(), personData.getFoedeland(),
                personData.getTalespraaktolk(), personData.getTegnspraaktolk(), personData.getTolkBehovSistOppdatert(),
                personData.getKommunenummer(), personData.getBydelsnummer(), personData.getUtenlandskAdresse(),
                personData.getBostedSistOppdatert(), personData.isHarUkjentBosted(), personData.getDiskresjonskode(),
                sikkerhetstiltak_type, sikkerhetstiltak_beskrivelse, sikkerhetstiltak_gyldigfra, sikkerhetstiltak_gyldigtil);

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
                                .setTalespraaktolk(rs.getString("talespraaktolk"))
                                .setTegnspraaktolk(rs.getString("tegnspraaktolk"))
                                .setTolkBehovSistOppdatert(toLocalDateOrNull(rs.getString("tolkBehovSistOppdatert")))
                                .setStatsborgerskap(statsborgerskaps)
                                .setBydelsnummer(rs.getString("bydelsnummer"))
                                .setKommunenummer(rs.getString("kommunenummer"))
                                .setUtenlandskAdresse(rs.getString("utenlandskAdresse"))
                                .setHarUkjentBosted(rs.getBoolean("harUkjentBosted"))
                                .setBostedSistOppdatert(toLocalDateOrNull(rs.getString("bostedSistOppdatert")))
                                .setDiskresjonskode(rs.getString("diskresjonkode"))
                                .setSikkerhetstiltak(
                                        new Sikkerhetstiltak(
                                                rs.getString("sikkerhetstiltak_type"),
                                                rs.getString("sikkerhetstiltak_beskrivelse"),
                                                toLocalDateOrNull(rs.getString("sikkerhetstiltak_gyldigfra")),
                                                toLocalDateOrNull(rs.getString("sikkerhetstiltak_gyldigtil"))
                                        ))
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

    public List<String> finnSkjulteBrukere(List<String> fnrListe, BrukerinnsynTilganger brukerInnsynTilganger) {
        String fnrsStr = fnrListe.stream().collect(Collectors.joining(",", "{", "}"));

        return dbReadOnly.queryForList(
                """
                SELECT freg_ident from bruker_data bd, nom_skjerming ns
                where ns.fodselsnr = bd.freg_ident AND freg_ident = ANY (?::varchar[])
                AND (
                    (diskresjonkode = '6' AND NOT ?::boolean)
                    OR (diskresjonkode = '7' AND NOT ?::boolean)
                    OR (er_skjermet AND NOT ?::boolean)
                )
                """, fnrsStr, brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig(), brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig(), brukerInnsynTilganger.tilgangTilSkjerming())
                .stream()
                .map(rs -> (String) rs.get("freg_ident"))
                .toList();
    }
}
