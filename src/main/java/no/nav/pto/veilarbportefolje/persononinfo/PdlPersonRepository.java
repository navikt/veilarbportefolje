package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

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

    public void slettLagretBrukerData(List<PDLIdent> identer) {
        String identerParam = identer.stream().map(PDLIdent::getIdent).collect(Collectors.joining(",", "{", "}"));
        db.update("DELETE from bruker_data where freg_ident = any (?::varchar[])", identerParam);
    }
}
