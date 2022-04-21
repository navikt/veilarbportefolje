package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PdlRepository {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    @Transactional
    public void upsertIdenter(List<PDLIdent> pdlIdenter) {
        List<String> identer = pdlIdenter.stream().map(PDLIdent::getIdent).toList();
        slettIdenter(identer);

        String nyLokalIdent = db.queryForObject("select nextval('PDL_PERSON_SEQ')", String.class);
        pdlIdenter.forEach(ident -> insertIdent(nyLokalIdent, ident));
    }

    @Transactional
    public void slettLokalIdentlagringHvisIkkeUnderOppfolging(AktorId aktorId) {
        String lokalIdent = hentLokalIdent(aktorId.get());
        if (harIdentUnderOppfolging(lokalIdent)) {
            log.info("""
                            Sletter ikke identer lagret på aktorId: {}.
                            Da en eller flere relaterte identer på lokalIdent: {} er under oppfolging.
                            """,
                    aktorId, lokalIdent);
            return;
        }
        log.info("Sletter identer lagret på aktorId: {}, lokalIdent: {}.", aktorId, lokalIdent);
        slettLagreteIdenter(lokalIdent);
    }

    public String hentLokalIdent(String lookUpIdent) {
        return queryForObjectOrNull(() -> db.queryForObject("select person from bruker_identer where IDENT = ?",
                (rs, row) -> rs.getString("person"), lookUpIdent));
    }

    private void insertIdent(String lokalIdent, PDLIdent ident) {
        db.update("insert into bruker_identer (person, ident, historisk, gruppe) VALUES (?, ?, ?, ?)",
                lokalIdent, ident.getIdent(), ident.isHistorisk(), ident.getGruppe().name());
    }

    private void slettIdenter(List<String> identer) {
        String identerParam = identer.stream().collect(Collectors.joining(",", "{", "}"));
        db.update("delete from bruker_identer where ident = any (?::varchar[])", identerParam);
    }

    private void slettLagreteIdenter(String lokaleIdent) {
        log.info("Sletter lokal ident: {}", lokaleIdent);
        db.update("delete from bruker_identer where person = ?", lokaleIdent);
    }

    private boolean harIdentUnderOppfolging(String lookUpIdent) {
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject("""
                        select bool_or(oppfolging) as harOppfolging from oppfolging_data
                        where aktoerid in (select ident from bruker_identer where person = ?)
                        """, (rs, row) -> rs.getBoolean("harOppfolging"), lookUpIdent))
        ).orElse(false);
    }
}
