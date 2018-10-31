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
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.DbUtils.getCauseString;
import static no.nav.fo.util.MetricsUtils.timed;

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
            + "FROM OPPFOLGING_DATA "
            + "WHERE OPPFOLGING = 'J' "
            + "AND AKTOERID NOT IN "
            + "(SELECT AKTOERID FROM AKTOERID_TO_PERSONID)";


    @Scheduled(cron = "${veilarbportefolje.schedule.oppdaterMapping.cron:0 0/5 * * * *}")
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
        timed("map.aktorid", () -> {
            log.debug("Starter mapping av aktørid");
            List<String> aktoerIder = db.query(IKKE_MAPPEDE_AKTORIDER, (RowMapper<String>) (rs, rowNum) -> rs.getString("AKTOERID"));
            log.info("Aktørider som skal mappes " + aktoerIder);
            aktoerIder.forEach((id) -> hentPersonidFraAktoerid(AktoerId.of(id)));
            log.info("Ferdig med mapping av [" + aktoerIder.size() + "] aktørider");
        });
    }

    public Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerId) {
        return brukerRepository.retrievePersonid(aktoerId)
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId)
                .onFailure(e -> log.warn("Kunne ikke hente/mappe personId for aktorid: {}: {}", aktoerId, getCauseString(e)));
    }

    private PersonId getPersonIdFromFnr(AktoerId aktoerId) {
        Fnr fnr = hentFnrViaSoap(aktoerId).get();
        PersonId nyPersonId = brukerRepository.retrievePersonidFromFnr(fnr).get();
        AktoerId nyAktorIdForPersonId = hentAktoeridFraFnr(fnr).get();

        updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(aktoerId, nyPersonId, nyAktorIdForPersonId);
        return nyPersonId;
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


    private Try<Fnr> hentFnrViaSoap(AktoerId aktoerId) {
        return Try.of(() -> aktorService.getFnr(aktoerId.toString()).get())
                .onFailure(e -> log.warn("Kunne ikke hente aktoerId for fnr : {}", getCauseString(e)))
                .map(Fnr::of);
    }

    @Transactional
    public void updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(AktoerId aktoerId, PersonId personId, AktoerId aktoerIdFraTPS) {
        if (personId == null) {
            return;
        }
        brukerRepository.setGjeldeneFlaggTilNull(personId);

        if (!aktoerId.equals(aktoerIdFraTPS)) {
            brukerRepository.insertGamleAktoerIdMedGjeldeneFlaggNull(aktoerId, personId);
        }

        brukerRepository.insertAktoeridToPersonidMapping(aktoerIdFraTPS, personId);
    }
}
