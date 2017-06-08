package no.nav.fo.service;

import javaslang.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.exception.FantIkkeFnrException;
import no.nav.fo.util.sql.where.WhereClause;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static no.nav.fo.util.sql.SqlUtils.upsert;

@Slf4j
public class AktoerServiceImpl implements AktoerService {

    @Inject
    private AktoerV2 endpoint;

    @Inject
    private JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    public Optional<String> hentPersonidFraAktoerid(String aktoerid) {
        Optional<String> personid = hentSingleFraDb(
                db,
                "SELECT PERSONID FROM AKTOERID_TO_PERSONID WHERE AKTOERID = ?",
                (data) -> ((String) data.get("personid")),
                aktoerid
        );

        return Optional.ofNullable(personid
                .orElseGet(() -> {
                    Try<WSHentIdentForAktoerIdResponse> response = Try.of(() -> endpoint.hentIdentForAktoerId(new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerid)));

                    Try<String> personId = Try.of(() -> brukerRepository
                            .retrievePersonidFromFnr(response.get().getIdent())
                            .map(BigDecimal::intValue)
                            .map(x -> Integer.toString(x))
                            .orElseThrow(() -> new FantIkkeFnrException(response.get().getIdent())));
                    brukerRepository.insertAktoeridToPersonidMapping(aktoerid, personId.get());
                    return personId.get();
                }));
    }

    private static <T> Optional<T> hentSingleFraDb(JdbcTemplate db, String sql, Function<Map<String, Object>, T> mapper, Object... args) {
        List<Map<String, Object>> data = db.queryForList(sql, args);
        if (data.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(mapper.apply(data.get(0)));
    }

    @Override
    public Optional<String> hentAktoeridFraPersonid(String personid) {
        return hentSingleFraDb(
                db,
                "SELECT aktoerid FROM PERSON_MAPPING WHERE personid = ?",
                (data) -> ((String) data.get("aktoerid")),
                personid
        );
    }

    @Override
    public Optional<String> hentAktoeridFraFnr(String fnr) {
        return Try.of(() -> endpoint.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest().withIdent(fnr)))
                .map(WSHentAktoerIdForIdentResponse::getAktoerId)
                .toJavaOptional();
    }

    @Override
    public Optional<String> hentFnrFraAktoerid(String aktoerid) {
        Optional<String> fnr = hentSingleFraDb(
                db,
                "SELECT fnr FROM PERSON_MAPPING WHERE aktoerid = ?",
                (data) -> ((String) data.get("fnr")),
                aktoerid
        );

        if (fnr.isPresent()) { return fnr; }

        return hentFnrFraAktoerV1(aktoerid);
    }

    private Optional<String> hentFnrFraAktoerV1(String aktoerid) {
        Optional<String> maybyFnr = Try.of(() -> endpoint.hentIdentForAktoerId(new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerid)))
                .map(WSHentIdentForAktoerIdResponse::getIdent)
                .toJavaOptional();

        maybyFnr.ifPresent((fnr) -> {
            upsert(db, "PERSON_MAPPING")
                    .set("fnr", fnr)
                    .where(WhereClause.equals("aktoerid", aktoerid))
                    .execute();
        });

        return maybyFnr;
    }
}
