package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKSHENDELSE.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateTimeOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakshendelseRepository {
    private final JdbcTemplate db;

    private static Tiltakshendelse tiltakshendelseMapper (Map<String, Object> rs) {
        return new Tiltakshendelse(
                (UUID) rs.get(ID),
                toLocalDateTimeOrNull((Timestamp) rs.get(OPPRETTET)),
                (String) rs.get(TEKST),
                (String) rs.get(LENKE),
                Tiltakstype.valueOf((String) rs.get(TILTAKSTYPE)),
                Fnr.of((String) rs.get(FNR))
        );
    }

    @Transactional
    public boolean tryLagreTiltakshendelseData(KafkaTiltakshendelse tiltakshendelseData) {
        return upsertTiltakshendelse(tiltakshendelseData);
    }

    public boolean upsertTiltakshendelse(KafkaTiltakshendelse tiltakshendelse) {
        try {
            db.update("""
                    INSERT INTO tiltakshendelse
                       (id, fnr, opprettet, tekst, lenke, tiltakstype_kode, avsender, sist_endret)          
                                      VALUES (?, ?, ?, ?, ?, ?, ?, now())
                    ON CONFLICT (id)
                    DO UPDATE SET (fnr, opprettet, tekst, lenke, tiltakstype_kode, avsender, sist_endret) = 
                       (excluded.fnr, excluded.opprettet, excluded.tekst, excluded.lenke, excluded.tiltakstype_kode, excluded.avsender, excluded.sist_endret)
                      """,
                    tiltakshendelse.id(), tiltakshendelse.fnr().toString(), tiltakshendelse.opprettet(), tiltakshendelse.tekst(), tiltakshendelse.lenke(), tiltakshendelse.tiltakstype().name(), tiltakshendelse.avsender().name()
            );
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<Tiltakshendelse> hentAlleTiltakshendelser()  {
        String sql = "SELECT * FROM tiltakshendelse";

        try {
            return db.queryForList(sql).stream().map(TiltakshendelseRepository::tiltakshendelseMapper).toList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
