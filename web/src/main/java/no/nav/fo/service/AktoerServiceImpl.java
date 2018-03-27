package no.nav.fo.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.MetricsUtils.timed;

import java.time.Instant;

@Slf4j
public class AktoerServiceImpl implements AktoerService {

    @Inject
    private AktorService aktorService;

    @Inject
    private JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private LockingTaskExecutor taskExecutor;
    
    @Value("${lock.aktoridmapping.seconds:3600}")
    private int lockAktoridmappingSeconds;

    private static final String IKKE_MAPPEDE_AKTORIDER = "SELECT AKTOERID "
            + "FROM VW_OPPFOLGING_DATA "
            + "WHERE AKTOERID NOT IN "
            + "(SELECT AKTOERID FROM AKTOERID_TO_PERSONID)";

    
    @Scheduled(
            fixedDelayString = "${veilarbportefolje.schedule.oppdaterMapping.millis:300000}", 
            initialDelayString = "${veilarbportefolje.schedule.oppdaterMapping.delay:60000}")
    private void scheduledOppdaterAktoerTilPersonIdMapping() {
        mapAktorIdWithLock();
    }

    private void mapAktorIdWithLock() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(lockAktoridmappingSeconds);
        Instant lockAtLeastUntil = Instant.now().plusSeconds(10);
        taskExecutor.executeWithLock(
                () -> mapAktorId(), 
                new LockConfiguration("oppdaterAktoerTilPersonIdMapping", lockAtMostUntil, lockAtLeastUntil));
    }

    void mapAktorId() {
        timed("map.aktorid", () ->   {
            log.debug("Starter mapping av aktørid");
            List<String> aktoerIder = db.query(IKKE_MAPPEDE_AKTORIDER, (RowMapper<String>) (rs, rowNum) -> rs.getString("AKTOERID"));
            log.info("Aktørider som skal mappes " + aktoerIder);
            aktoerIder.forEach((id) -> hentPersonidFraAktoerid(AktoerId.of(id)));
            log.info("Ferdig med mapping av [" + aktoerIder.size() + "] aktørider");
        });
    }

    public Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerId) {
        Try<PersonId> personid = brukerRepository.retrievePersonid(aktoerId);

        if (personid.isSuccess() && personid.get() == null) {
            return hentPersonIdViaSoap(aktoerId);
        }
        return personid;
    }

    Try<AktoerId> hentAktoeridFraPersonid(PersonId personId) {
        return hentSingleFraDb(
                db,
                "SELECT AKTOERID FROM AKTOERID_TO_PERSONID WHERE PERSONID = ?",
                (data) -> AktoerId.of((String) data.get("aktoerid")),
                personId.toString()
        ).orElse(() -> brukerRepository.retrieveFnrFromPersonid(personId)
                .flatMap(this::hentAktoeridFraFnr))
                .onSuccess(aktoerId -> brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId));
    }

    @Override
    public Try<AktoerId> hentAktoeridFraFnr(Fnr fnr) {
        return Try.of(() -> aktorService.getAktorId(fnr.toString()).get())
                .map(AktoerId::of);
    }

    public Map<Fnr, Optional<PersonId>> hentPersonidsForFnrs(List<Fnr> fnrs) {
        Map<Fnr, Optional<PersonId>> typeMap = new HashMap<>();
        Map<String, Optional<String>> stringMap = brukerRepository.retrievePersonidFromFnrs(fnrs.stream().map(Fnr::toString).collect(toList()));
        stringMap.forEach((key, value) -> typeMap.put(new Fnr(key), value.map(PersonId::of)));
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

    @Override
    public Map<AktoerId, Optional<PersonId>> hentPersonidsForAktoerids(List<AktoerId> aktoerIds) {
        Map<AktoerId, Optional<PersonId>> aktoerIdToPersonidMap = new HashMap<>(aktoerIds.size());
        Map<AktoerId, Optional<PersonId>> fromDb = brukerRepository.hentPersonidsFromAktoerids(aktoerIds);

        fromDb.forEach((key, value) -> {
            if(value.isPresent()) {
                aktoerIdToPersonidMap.put(key, value);
            } else {
                aktoerIdToPersonidMap.put(key, (hentPersonIdViaSoap(key)).toJavaOptional());
            }
        });
        return aktoerIdToPersonidMap;
    }

    private Try<PersonId> hentPersonIdViaSoap(AktoerId aktoerId) {
        return hentFnrViaSoap(aktoerId)
                .flatMap(brukerRepository::retrievePersonidFromFnr)
                .andThen(personId -> brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId))
                .onFailure(e -> log.warn("Kunne ikke finne personId for aktoerId {}.", aktoerId));
    }

    private Try<Fnr> hentFnrViaSoap(AktoerId aktoerId) {
        return Try.of(() -> aktorService.getFnr(aktoerId.toString()).get())
                .map(Fnr::of);
    }

    private static <T> Try<T> hentSingleFraDb(JdbcTemplate db, String sql, Function<Map<String, Object>, T> mapper, Object... args) {
        List<Map<String, Object>> data = db.queryForList(sql, args);
        if (data.size() != 1) {
            return Try.failure(new RuntimeException("Kunne ikke hente single fra Db"));
        }
        return Try.success(mapper.apply(data.get(0)));
    }
}
