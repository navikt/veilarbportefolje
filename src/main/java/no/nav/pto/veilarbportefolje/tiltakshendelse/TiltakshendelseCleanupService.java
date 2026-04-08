package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltakshendelseCleanupService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void slettConsumerRecordOgTiltakshendelse(UUID tiltakshendelseId, String fnr) {
        // 1) Slett consumer record (tilpass WHERE til riktig kolonne/format)
        int deletedConsumer = jdbcTemplate.update(
                "DELETE FROM kafka_consumer_record WHERE key = ?",
                tiltakshendelseId.toString()
        );

        // 2) Slett tiltakshendelse (bruk gjerne fnr også hvis du vil være ekstra safe)
        int deletedTiltak = jdbcTemplate.update(
                "DELETE FROM tiltakshendelse WHERE id = ? AND fnr = ?",
                tiltakshendelseId, fnr
        );

        log.info("Cleanup: deleted kafka_consumer_record={}, tiltakshendelse={} for id={}", deletedConsumer, deletedTiltak, tiltakshendelseId);
    }
}
