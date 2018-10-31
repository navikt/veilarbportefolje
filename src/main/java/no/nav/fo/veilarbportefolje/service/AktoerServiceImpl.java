package no.nav.fo.veilarbportefolje.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.Fnr;
import no.nav.fo.veilarbportefolje.domene.PersonId;
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
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;
import static no.nav.fo.veilarbportefolje.util.DbUtils.getCauseString;

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

    private static final String IKKE_MAPPEDE_AKTORIDER = "SELECT AKTOERID "
            + "FROM OPPFOLGING_DATA "
            + "WHERE OPPFOLGING = 'J' "
            + "AND AKTOERID NOT IN "
            + "(SELECT AKTOERID FROM AKTOERID_TO_PERSONID)";


    @Scheduled(cron = "0 0/5 * * * *")
    private void scheduledOppdaterAktoerTilPersonIdMapping() {
        mapAktorIdWithLock();
    }

    private void mapAktorIdWithLock() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(3600);
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
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId);
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
