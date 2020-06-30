package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.util.JobUtils.runAsyncJobOnLeader;

@Slf4j
public class AktoerServiceImpl implements AktoerService {

    private AktorregisterClient aktorregisterClient;
    private JdbcTemplate db;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;

    private static final String IKKE_MAPPEDE_AKTORIDER = "SELECT AKTOERID "
            + "FROM OPPFOLGING_DATA "
            + "WHERE OPPFOLGING = 'J' "
            + "AND AKTOERID NOT IN "
            + "(SELECT AKTOERID FROM AKTOERID_TO_PERSONID)";


    @Scheduled(cron = "0 0/5 * * * *")
    private void scheduledOppdaterAktoerTilPersonIdMapping() {
        runAsyncJobOnLeader(this::mapAktorId);
    }

    void mapAktorId() {
        List<String> aktoerIder = db.query(IKKE_MAPPEDE_AKTORIDER, (rs, rowNum) -> rs.getString("AKTOERID"));
        log.info("Aktørider som skal mappes " + aktoerIder);

        aktoerIder.forEach((id) -> {
            AktoerId aktoerId = AktoerId.of(id);
            hentPersonidFraAktoerid(aktoerId);
            elasticIndexer.indekserAsynkront(aktoerId);
        });

        log.info("Ferdig med mapping av [" + aktoerIder.size() + "] aktørider");
    }

    public Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerId) {
        return brukerRepository.retrievePersonid(aktoerId)
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId)
                .onFailure(e -> log.warn("Kunne ikke hente/mappe personId for aktorid: " + aktoerId, e));
    }

    private PersonId getPersonIdFromFnr(AktoerId aktoerId) {
        Fnr fnr = hentFnrFraAktorId(aktoerId);
        PersonId nyPersonId = brukerRepository.retrievePersonidFromFnr(fnr).get();
        AktoerId nyAktorIdForPersonId = hentAktoeridFraFnr(fnr).get();

        updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(aktoerId, nyPersonId, nyAktorIdForPersonId);
        return nyPersonId;
    }

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

    @Deprecated
    public Fnr hentFnrFraAktorId(AktoerId aktoerId) {
        return Fnr.of(aktorregisterClient.hentFnr(aktoerId.toString()));
    }

    @Transactional
    void updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(AktoerId aktoerId, PersonId personId, AktoerId aktoerIdFraTPS) {
        if (personId == null) {
            return;
        }

        if (!aktoerId.equals(aktoerIdFraTPS)) {
            brukerRepository.insertGamleAktoerIdMedGjeldeneFlaggNull(aktoerId, personId);
        } else {
            brukerRepository.setGjeldeneFlaggTilNull(personId);
            brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId);
        }


    }
}
