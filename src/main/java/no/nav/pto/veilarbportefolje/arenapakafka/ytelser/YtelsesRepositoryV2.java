package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class YtelsesRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsert(AktorId aktorId, TypeKafkaYtelse type, YtelsesInnhold innhold) {
        Timestamp startdato = getTimestampOrNull(innhold.getFraOgMedDato(), false);
        Timestamp utlopsdato = getTimestampOrNull(innhold.getTilOgMedDato(), true);

        db.update("""
                INSERT INTO YTELSER
                (VEDTAKSID, AKTORID, PERSONID, YTELSESTYPE, SAKSID, SAKSTYPEKODE, RETTIGHETSTYPEKODE,
                STARTDATO, UTLOPSDATO, ANTALLUKERIGJEN, ANTALLPERMITTERINGSUKER, ANTALLUKERIGJENUNNTAK)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (VEDTAKSID)
                DO UPDATE SET (AKTORID, PERSONID, YTELSESTYPE, SAKSID, SAKSTYPEKODE, RETTIGHETSTYPEKODE,
                STARTDATO, UTLOPSDATO, ANTALLUKERIGJEN, ANTALLPERMITTERINGSUKER, ANTALLUKERIGJENUNNTAK) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                innhold.getVedtakId(), aktorId.get(), innhold.getPersonId(),
                    type.toString(), innhold.getSaksId(), innhold.getSakstypeKode(), innhold.getRettighetstypeKode(), startdato, utlopsdato,
                    innhold.getAntallUkerIgjen(), innhold.getAntallUkerIgjenUnderPermittering(), innhold.getAntallDagerIgjenUnntak(),
                aktorId.get(), innhold.getPersonId(),
                    type.toString(), innhold.getSaksId(), innhold.getSakstypeKode(), innhold.getRettighetstypeKode(), startdato, utlopsdato,
                    innhold.getAntallUkerIgjen(), innhold.getAntallUkerIgjenUnderPermittering(), innhold.getAntallDagerIgjenUnntak()
                );
    }

    public List<YtelseDAO> getYtelser(AktorId aktorId) {
        if (aktorId == null) {
            return new ArrayList<>();
        }

        final String hentRadSql = "SELECT * FROM YTELSER WHERE AKTORID = ?";

        return db.queryForList(hentRadSql, aktorId.get())
                .stream().map(this::mapTilYtelseDAO)
                .collect(toList());
    }

    private YtelseDAO mapTilYtelseDAO(Map<String, Object> row) {
        return new YtelseDAO()
                .setAktorId(AktorId.of((String) row.get(AKTORID)))
                .setPersonId(PersonId.of((String) row.get(PERSONID)))
                .setSaksId((String) row.get(SAKSID))
                .setType(TypeKafkaYtelse.valueOf((String) row.get(YTELSESTYPE)))
                .setSakstypeKode((String) row.get(SAKSTYPEKODE))
                .setRettighetstypeKode((String) row.get(RETTIGHETSTYPEKODE))
                .setUtlopsDato((Timestamp) row.get(UTLOPSDATO))
                .setStartDato((Timestamp) row.get(STARTDATO))
                .setAntallUkerIgjen((Integer) row.get(ANTALLUKERIGJEN))
                .setAntallUkerIgjenPermittert((Integer) row.get(ANTALLPERMITTERINGSUKER))
                .setAntallDagerIgjenUnntak((Integer) row.get(ANTALLUKERIGJENUNNTAK));
    }

    public void slettYtelse(String vedtaksId) {
        if (vedtaksId == null) {
            return;
        }
        log.info("Sletter ytelse: {}", vedtaksId);
        db.update("DELETE FROM YTELSER WHERE VEDTAKSID = ?", vedtaksId);
//        SqlUtils.delete(db, TABLE_NAME)
//                .where(WhereClause.equals(VEDTAKID, vedtakId))
//                .execute();
    }

    private Timestamp getTimestampOrNull(ArenaDato date, boolean tilOgMedDato) {
        if (date == null || date.getLocalDate() == null) {
            return null;
        }
        if (tilOgMedDato) {
            return Timestamp.valueOf(date.getLocalDate().plusHours(23).plusMinutes(59));
        }
        return Timestamp.valueOf(date.getLocalDate());
    }

    public List<AktorId> hentBrukereMedYtelserSomStarterIDag() {
        final String brukereSomStarterIDag = "SELECT distinct AKTORID FROM YTELSER WHERE trunc(STARTDATO) = trunc(current_timestamp)";

        return db.queryForList(brukereSomStarterIDag, AktorId.class);
    }
}
