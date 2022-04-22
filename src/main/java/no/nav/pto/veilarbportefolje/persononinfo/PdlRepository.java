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
    public void upsertIdenter(List<PDLIdent> identer) {
        List<String> personer = hentPersoner(identer);
        personer.forEach(this::slettLagretePerson);

        String nyLokalIdent = db.queryForObject("select nextval('PDL_PERSON_SEQ')", String.class);
        identer.forEach(ident -> insertIdent(nyLokalIdent, ident));
    }

    @Transactional
    public void slettLokalIdentlagringHvisIkkeUnderOppfolging(AktorId aktorId) {
        String lokalIdent = hentPerson(aktorId.get());
        if (harIdentUnderOppfolging(lokalIdent)) {
            log.warn("""
                            Sletter ikke identer tilknyttet aktorId: {}.
                            Da en eller flere relaterte identer på person: {} er under oppfolging.
                            """,
                    aktorId, lokalIdent);
            return;
        }
        log.info("Sletter identer lagret på aktorId: {}, lokalIdent: {}.", aktorId, lokalIdent);
        slettLagretePerson(lokalIdent);
    }

    public String hentPerson(String lookUpIdent) {
        return queryForObjectOrNull(() -> db.queryForObject("select person from bruker_identer where IDENT = ?",
                (rs, row) -> rs.getString("person"), lookUpIdent));
    }

    public List<String> hentPersoner(List<PDLIdent> identer) {
        String identerParam = identer.stream().map(PDLIdent::getIdent).collect(Collectors.joining(",", "{", "}"));
        return db.queryForList("select person from bruker_identer where ident = any (?::varchar[])", identerParam)
                .stream().map(rs -> (String) rs.get("person")).toList();
    }

    private void insertIdent(String person, PDLIdent ident) {
        db.update("insert into bruker_identer (person, ident, historisk, gruppe) VALUES (?, ?, ?, ?)",
                person, ident.getIdent(), ident.isHistorisk(), ident.getGruppe().name());
    }

    private void slettLagretePerson(String person) {
        log.info("Sletter lokal ident: {}", person);
        db.update("delete from bruker_identer where person = ?", person);
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
