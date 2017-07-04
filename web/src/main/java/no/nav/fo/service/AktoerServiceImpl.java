package no.nav.fo.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.sql.where.WhereClause;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static no.nav.fo.util.sql.SqlUtils.upsert;

@Slf4j
public class AktoerServiceImpl implements AktoerService {

    @Inject
    private AktoerV2 endpoint;

    @Inject
    private JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    public Optional<String> hentPersonidFraAktoerid(AktoerId aktoerId) {
        return
                brukerRepository
                        .retrievePersonid(aktoerId)
                        .orElse(trySoapService(aktoerId))
                        .map(PersonId::toString)
                        .toJavaOptional();
    }

    private Supplier<Try<? extends PersonId>> trySoapService(AktoerId aktoerId) {
        return () -> hentFnrViaSoap(aktoerId)
                .flatMap(brukerRepository::retrievePersonidFromFnr)
                .andThen(personId -> brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId))
                .onFailure(e -> log.warn("Kunne ikke finne personId for aktoerId {}.", aktoerId));
    }

    private Try<Fnr> hentFnrViaSoap(AktoerId aktoerId) {
        WSHentIdentForAktoerIdRequest soapRequest = new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerId.toString());

        return
                Try.of(
                        () -> endpoint.hentIdentForAktoerId(soapRequest))
                        .map(WSHentIdentForAktoerIdResponse::getIdent)
                        .map(Fnr::new)
                        .onFailure(e -> log.warn("SOAP-Kall mot aktoerService (AktoerV2) feilet: {}", e.getMessage())
                        );
    }

    private static <T> Optional<T> hentSingleFraDb(JdbcTemplate db, String sql, Function<Map<String, Object>, T> mapper, Object... args) {
        List<Map<String, Object>> data = db.queryForList(sql, args);
        if (data.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(mapper.apply(data.get(0)));
    }

    @Override
    public Optional<AktoerId> hentAktoeridFraPersonid(String personid) {
        return hentSingleFraDb(
                db,
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ?",
                (data) -> (String) data.get("aktoerid"),
                personid
        ).map(AktoerId::new);
    }

    @Override
    public Optional<AktoerId> hentAktoeridFraFnr(Fnr fnr) {
        return Try.of(() -> endpoint.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest().withIdent(fnr.toString())))
                .map(WSHentAktoerIdForIdentResponse::getAktoerId)
                .toJavaOptional()
                .map(AktoerId::new);
    }

    @Override
    public Optional<Fnr> hentFnrFraAktoerid(AktoerId aktoerid) {
        Optional<String> fnr = hentSingleFraDb(
                db,
                "SELECT FNR FROM BRUKER_DATA WHERE AKTOERID = ?",
                (data) -> (String) data.get("FNR"),
                aktoerid
        );

        if (fnr.isPresent()) {
            return fnr.map(Fnr::new);
        }

        return hentFnrFraAktoerV1(aktoerid.toString());
    }

    private Optional<Fnr> hentFnrFraAktoerV1(String aktoerid) {
        Optional<String> maybeFnr = Try.of(() -> endpoint.hentIdentForAktoerId(new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerid)))
                .map(WSHentIdentForAktoerIdResponse::getIdent)
                .toJavaOptional();

        maybeFnr.ifPresent((fnr) -> {
            upsert(db, "BRUKER_DATA")
                    .set("FNR", fnr)
                    .where(WhereClause.equals("AKTOERID", aktoerid))
                    .execute();
        });

        return maybeFnr.map(Fnr::new);
    }
}
