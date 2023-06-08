package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PdlIdentRepository {
    private final JdbcTemplate db;

    @Transactional
    public void upsertIdenter(List<PDLIdent> identer) {
        List<String> personer = hentPersoner(identer);
        personer.forEach(this::slettLagretePerson);

        String nyLokalIdent = db.queryForObject("select nextval('PDL_PERSON_SEQ')", String.class);
        identer.forEach(ident -> insertIdent(nyLokalIdent, ident));
    }

    public boolean harAktorIdUnderOppfolging(List<AktorId> identer) {
        String identerParam = identer.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject("""
                        select bool_or(oppfolging) as harOppfolging from oppfolging_data
                        where aktoerid = any (?::varchar[])
                        """, (rs, row) -> rs.getBoolean("harOppfolging"), identerParam))
        ).orElse(false);
    }

    public boolean erBrukerUnderOppfolging(String brukerId) {
        return Optional.ofNullable(queryForObjectOrNull(() -> db.queryForObject("""
                select bool_or(oppfolging) as harOppfolging from oppfolging_data
                        where aktoerid in (
                            select bi2.ident
                            from bruker_identer bi1
                            inner join bruker_identer bi2 on bi2.person = bi1.person
                            where bi1.ident = ? and bi2.gruppe = 'AKTORID')
                """, Boolean.class, brukerId))).orElse(false);
    }

    public List<PDLIdent> hentIdenter(String ident) {
        return db.queryForList("select * from bruker_identer where person = ?", ident)
                .stream()
                .map(PdlIdentRepository::mapTilident)
                .toList();
    }

    public IdenterForBruker hentIdenterForBruker(String brukerId) {
        List<String> identer = db.queryForList("""
                select bi2.ident
                from bruker_identer bi1
                inner join bruker_identer bi2 on bi2.person = bi1.person
                where bi1.ident = ?
                """, String.class, brukerId);

        return new IdenterForBruker(identer);
    }

    @SneakyThrows
    public static PDLIdent mapTilident(Map<String, Object> rs) {
        return new PDLIdent()
                .setIdent((String) rs.get("ident"))
                .setHistorisk((Boolean) rs.get("historisk"))
                .setGruppe(PDLIdent.Gruppe.valueOf((String) rs.get("gruppe")));
    }

    public String hentPerson(String lookUpIdent) {
        return queryForObjectOrNull(() -> db.queryForObject("select person from bruker_identer where ident = ?",
                (rs, row) -> rs.getString("person"), lookUpIdent));
    }

    private List<String> hentPersoner(List<PDLIdent> identer) {
        String identerParam = identer.stream().map(PDLIdent::getIdent).collect(Collectors.joining(",", "{", "}"));
        return db.queryForList("select person from bruker_identer where ident = any (?::varchar[])", identerParam)
                .stream().map(rs -> (String) rs.get("person")).toList();
    }

    public Fnr hentFnr(AktorId aktorId) {
        return queryForObjectOrNull(
                () -> db.queryForObject("select fnr from aktive_identer where aktorid = ?",
                        (rs, i) -> Optional.ofNullable(rs.getString("fnr")).map(Fnr::of).orElse(null),
                        aktorId.get()
                ));
    }

    public AktorId hentAktorId(Fnr fnr) {
        return queryForObjectOrNull(
                () -> db.queryForObject("select aktorid from aktive_identer where fnr = ?",
                        (rs, i) -> Optional.ofNullable(rs.getString("aktorid")).map(AktorId::of).orElse(null),
                        fnr.get())
        );
    }

    private void insertIdent(String person, PDLIdent ident) {
        db.update("insert into bruker_identer (person, ident, historisk, gruppe) VALUES (?, ?, ?, ?)",
                person, ident.getIdent(), ident.isHistorisk(), ident.getGruppe().name());
    }

    public void slettLagretePerson(String person) {
        secureLog.info("Sletter lokal ident: {}", person);
        db.update("delete from bruker_identer where person = ?", person);
    }

    // TODO: 24/04/2023 : implement logic for this function
    public boolean erEndringForBarnAvBrukerUnderOppfolging(List<AktorId> aktorIder) {
        return true;
    }
}
