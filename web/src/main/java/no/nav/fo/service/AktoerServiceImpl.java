package no.nav.fo.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Slf4j
public class AktoerServiceImpl implements AktoerService {

    @Inject
    private AktoerV2 soapService;

    @Inject
    private JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    public Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerId) {
        Try<PersonId> personid = brukerRepository.retrievePersonid(aktoerId);

        if (personid.isSuccess() && personid.get() == null) {
            return hentPersonIdViaSoap(aktoerId);
        }
        return personid;
    }

    @Override
    public Try<AktoerId> hentAktoeridFraPersonid(PersonId personId) {
        return hentSingleFraDb(
                db,
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ?",
                (data) -> AktoerId.of((String) data.get("aktoerid")),
                personId.toString()
        ).orElse(() -> hentFnrFraPersonid(personId)
                .flatMap(this::hentAktoeridFraFnr))
                .onSuccess(aktoerId -> brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId));
    }

    @Override
    public Try<AktoerId> hentAktoeridFraFnr(Fnr fnr) {
        return Try.of(() -> soapService.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest().withIdent(fnr.toString())))
                .map(WSHentAktoerIdForIdentResponse::getAktoerId)
                .map(AktoerId::new);
    }

    @Override
    public Try<PersonId> hentPersonidFromFnr(Fnr fnr) {
        return hentSingleFraDb(
                db,
                "SELECT PERSON_ID FROM OPPFOLGINGSBRUKER WHERE FODSELSNR = ?",
                (data) -> String.valueOf(((Number) data.get("person_id")).intValue()),
                fnr.toString()
        ).map(PersonId::new);
    }

    @Override
    public Try<Fnr> hentFnrFraAktoerid(AktoerId aktoerId) {
        return hentFnrViaSoap(aktoerId);
    }

    @Override
    public Try<Fnr> hentFnrFraPersonid(PersonId personId) {
        return hentSingleFraDb(
                db,
                "SELECT FODSELSNR FROM OPPFOLGINGSBRUKER WHERE PERSON_ID = ?",
                (data) -> Fnr.of((String) data.get("fodselsnr")),
                personId.toString()
        );
    }

    public Map<Fnr, Optional<PersonId>> hentPersonidsForFnrs(List<Fnr> fnrs) {
        Map<Fnr, Optional<PersonId>> typeMap = new HashMap<>();
        Map<String, Optional<String>> stringMap = brukerRepository.retrievePersonidFromFnrs(fnrs.stream().map(Fnr::toString).collect(toList()));
        stringMap.forEach((key, value) -> typeMap.put(new Fnr(key), value.map(PersonId::new)));
        return typeMap;
    }

    @Override
    public Map<PersonId, Optional<AktoerId>> hentAktoeridsForPersonids(List<PersonId> personIds) {
        Map<PersonId, Optional<AktoerId>> personIdToAktoeridMap = new HashMap<>(personIds.size());
        Map<PersonId, Optional<AktoerId>> fromDb = brukerRepository.hentAktoeridsForPersonids(personIds);

        fromDb.forEach((key, value) -> {
                    if (value.isPresent()) {
                        personIdToAktoeridMap.put(key, value);
                    } else {
                        personIdToAktoeridMap.put(key, hentAktoeridFraPersonid(key).toJavaOptional());
                    }
                }
        );

        return personIdToAktoeridMap;
    }

    private Try<PersonId> hentPersonIdViaSoap(AktoerId aktoerId) {
        return hentFnrViaSoap(aktoerId)
                .flatMap(brukerRepository::retrievePersonidFromFnr)
                .andThen(personId -> brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId))
                .onFailure(e -> log.warn("Kunne ikke finne personId for aktoerId {}.", aktoerId));
    }

    private Try<Fnr> hentFnrViaSoap(AktoerId aktoerId) {
        WSHentIdentForAktoerIdRequest soapRequest = new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerId.toString());

        return
                Try.of(
                        () -> soapService.hentIdentForAktoerId(soapRequest))
                        .map(WSHentIdentForAktoerIdResponse::getIdent)
                        .map(Fnr::new)
                        .onFailure(e -> log.warn("SOAP-Kall mot baksystem (AktoerV2) feilet for aktoerId {} | {}", aktoerId, e.getMessage())
                        );
    }

    private static <T> Try<T> hentSingleFraDb(JdbcTemplate db, String sql, Function<Map<String, Object>, T> mapper, Object... args) {
        List<Map<String, Object>> data = db.queryForList(sql, args);
        if (data.size() != 1) {
            return Try.failure(new RuntimeException("Kunne ikke hente single fra Db"));
        }
        return Try.success(mapper.apply(data.get(0)));
    }
}
