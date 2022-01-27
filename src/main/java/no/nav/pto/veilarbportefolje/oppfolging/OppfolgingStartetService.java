package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static no.nav.pto.veilarbportefolje.database.Table.AKTIVITETER.AKTOERID;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OppfolgingStartetService extends KafkaCommonConsumerService<OppfolgingStartetDTO> {
    private final JdbcTemplate db;
    private final OppfolgingRepository oppfolgingRepository;
    private final OpensearchIndexer opensearchIndexer;
    private final BrukerRepository brukerRepository;
    private final AktorClient aktorClient;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(OppfolgingStartetDTO dto) {
        mapAktoerTilPersonId(dto.getAktorId());

        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        oppfolgingRepositoryV2.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        opensearchIndexer.indekser(dto.getAktorId());
        log.info("Bruker {} har startet oppfølging: {}", dto.getAktorId(), dto.getOppfolgingStartet());
    }

    private void mapAktoerTilPersonId(AktorId aktorId) {
        List<AktorId> mappedePersonIder = db.queryForList("SELECT PERSONID FROM AKTOERID_TO_PERSONID WHERE GJELDENE = 1 AND AKTOERID = ?",
                        aktorId.get())
                .stream()
                .map(map -> AktorId.of((String) map.get(AKTOERID)))
                .toList();
        if (mappedePersonIder.size() == 0) {
            brukerRepository.retrievePersonidFromFnr(aktorClient.hentFnr(aktorId))
                    .onFailure(e ->
                            log.info("(Test) Fant ikke personId i lenke ved oppfolging startet: {}", aktorId)
                    ).onSuccess(personId ->
                            log.info("(Test) Fant personId i lenke ved oppfolging startet: {}", aktorId)
                    );
        }
        if (mappedePersonIder.size() > 1) {
            log.warn("Det var flere mappet en personId for aktoer: {}", aktorId.get());
        }
    }
}
