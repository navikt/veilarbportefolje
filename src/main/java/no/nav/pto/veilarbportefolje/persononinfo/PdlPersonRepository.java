package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PdlPersonRepository {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsertPerson(PDLPerson personData) {
        db.update("""
                        INSERT INTO bruker_data (fnr, fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedsels_dato)
                        values (?,?,?,?,?,?,?)
                        on conflict (fnr)
                        do update set (fornavn, etternavn, mellomnavn, kjoenn, er_doed, foedsels_dato) =
                        (excluded.fornavn, excluded.etternavn, excluded.mellomnavn, excluded.kjoenn, excluded.er_doed, excluded.foedsels_dato)
                        """,
                personData.getFnr().get(), personData.getFornavn(), personData.getEtternavn(), personData.getMellomnavn(),
                personData.getKjonn().name(), personData.isErDoed(), personData.getFoedsel());
    }
}
