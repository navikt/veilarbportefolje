package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Repository
@RequiredArgsConstructor
public class PdlPersonRepository {
    private final JdbcTemplate db;

    public void upsertPerson(Fnr fnr, PDLPerson personData) {
        db.update("""
                        INSERT INTO bruker_data (freg_ident, fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedselsdato)
                        values (?,?,?,?,?,?,?)
                        on conflict (freg_ident)
                        do update set (fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedselsdato) =
                        (excluded.fornavn, excluded.etternavn, excluded.mellomnavn, excluded.kjoenn, excluded.er_doed, excluded.foedselsdato)
                        """,
                fnr.get(), personData.getFornavn(), personData.getEtternavn(), personData.getMellomnavn(),
                personData.getKjonn().name(), personData.isErDoed(), personData.getFoedsel());
    }

    public void slettLagretBrukerData(List<Fnr> identer) {
        if (identer.isEmpty()) {
            return;
        }
        String identerParam = identer.stream().map(Fnr::get).collect(Collectors.joining(",", "{", "}"));
        db.update("DELETE from bruker_data where freg_ident = any (?::varchar[])", identerParam);
    }

    @SneakyThrows
    public PDLPerson hentPerson(Fnr hentAktivFnr) {
        return queryForObjectOrNull(() ->
                db.queryForObject("select * from bruker_data where freg_ident = ?",
                        (rs, row) -> new PDLPerson()
                                .setFornavn(rs.getString("fornavn"))
                                .setEtternavn(rs.getString("etternavn"))
                                .setMellomnavn(rs.getString("mellomnavn"))
                                .setKjonn(Kjonn.valueOf(rs.getString("kjoenn")))
                                .setErDoed(rs.getBoolean("er_doed"))
                                .setFoedsel(rs.getDate("foedselsdato").toLocalDate())
                        ,
                        hentAktivFnr.get())
        );
    }
}
