package no.nav.pto.veilarbportefolje.scheduled;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

import static no.nav.pto.veilarbportefolje.util.JobUtils.runAsyncJobOnLeader;
@Slf4j
@Service
public class PersonIdToAktorIdSchedule {
    private final JdbcTemplate db;
    private final ElasticIndexer elasticIndexer;
    private final LeaderElectionClient leaderElectionClient;
    private final BrukerService brukerService;

    private static final String IKKE_MAPPEDE_AKTORIDER = "SELECT AKTOERID "
            + "FROM OPPFOLGING_DATA "
            + "WHERE OPPFOLGING = 'J' "
            + "AND AKTOERID NOT IN "
            + "(SELECT AKTOERID FROM AKTOERID_TO_PERSONID)";

    @Autowired
    public PersonIdToAktorIdSchedule(BrukerService brukerService, JdbcTemplate db, ElasticIndexer elasticIndexer, LeaderElectionClient leaderElectionClient) {
        this.brukerService = brukerService;
        this.db = db;
        this.elasticIndexer = elasticIndexer;
        this.leaderElectionClient = leaderElectionClient;
    }


    @Scheduled(cron = "0 0/5 * * * *")
    private void scheduledOppdaterAktoerTilPersonIdMapping() {
        runAsyncJobOnLeader(this::mapAktorId, leaderElectionClient);
    }

    void mapAktorId() {
        List<String> aktoerIder = db.query(IKKE_MAPPEDE_AKTORIDER, (rs, rowNum) -> rs.getString("AKTOERID"));
        log.info("Aktørider som skal mappes " + aktoerIder);

        aktoerIder.forEach((id) -> {
            AktoerId aktoerId = AktoerId.of(id);
            brukerService.hentPersonidFraAktoerid(aktoerId);
            elasticIndexer.indekser(aktoerId);
        });

        log.info("Ferdig med mapping av [" + aktoerIder.size() + "] aktørider");
    }

}