package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.Table.YTELSER.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class YtelsesRepository {
    private final JdbcTemplate db;

    public void upsertYtelse(AktorId aktorId, TypeKafkaYtelse type, YtelsesInnhold innhold) {
        Timestamp startDato = getTimestampOrNull(innhold.getFraOgMedDato(), false);
        Timestamp utlopsdato = getTimestampOrNull(innhold.getTilOgMedDato(), true);

        SqlUtils.upsert(db, TABLE_NAME)
                .set(VEDTAKID, innhold.getVedtakId())
                .set(AKTOERID, aktorId.get())
                .set(PERSONID, innhold.getPersonId())
                .set(YTELSESTYPE, type.toString())
                .set(SAKSID, innhold.getSaksId())
                .set(SAKSTYPEKODE, innhold.getSakstypeKode())
                .set(RETTIGHETSTYPEKODE, innhold.getRettighetstypeKode())
                .set(UTLOPSDATO, utlopsdato)
                .set(STARTDATO, startDato)
                .set(ANTALLUKERIGJEN, innhold.getAntallUkerIgjen())
                .set(ANTALLPERMITTERINGUKER, innhold.getAntallUkerIgjenUnderPermittering())
                .set(ANTALLUKERIGJENUNNTAK, innhold.getAntallDagerIgjenUnntak())
                .where(WhereClause.equals(VEDTAKID, innhold.getVedtakId()))
                .execute();
    }

    public List<YtelseDAO> getYtelser(AktorId aktorId) {
        if (aktorId == null) {
            return new ArrayList<>();
        }

        final String sql = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + AKTOERID + " = ?";

        return db.queryForList(sql, aktorId.get())
                .stream().map(this::mapTilYtelseDOA)
                .collect(toList());
    }

    private YtelseDAO mapTilYtelseDOA(Map<String, Object> row) {
        return new YtelseDAO()
                .setAktorId(AktorId.of((String) row.get(AKTOERID)))
                .setPersonId(PersonId.of((String) row.get(PERSONID)))
                .setSaksId((String) row.get(SAKSID))
                .setType(TypeKafkaYtelse.valueOf((String) row.get(YTELSESTYPE)))
                .setSakstypeKode((String) row.get(SAKSTYPEKODE))
                .setRettighetstypeKode((String) row.get(RETTIGHETSTYPEKODE))
                .setUtlopsDato((Timestamp) row.get(UTLOPSDATO))
                .setStartDato((Timestamp) row.get(STARTDATO))
                .setAntallUkerIgjen(((BigDecimal) row.get(ANTALLUKERIGJEN)).intValue())
                .setAntallUkerIgjenPermittert(((BigDecimal) row.get(ANTALLPERMITTERINGUKER)).intValue())
                .setAntallDagerIgjenUnntak(((BigDecimal) row.get(ANTALLUKERIGJENUNNTAK)).intValue());
    }

    public void slettYtelse(String vedtakId) {
        if (vedtakId == null) {
            return;
        }
        log.info("Sletter ytelse: {}", vedtakId);
        SqlUtils.delete(db, TABLE_NAME)
                .where(WhereClause.equals(VEDTAKID, vedtakId))
                .execute();
    }

    private Timestamp getTimestampOrNull(ArenaDato date, boolean tilOgMedDato) {
        if (date == null) {
            return null;
        }
        if (tilOgMedDato) {
            return Timestamp.valueOf(date.getLocalDate().plusHours(23).plusMinutes(59));
        }
        return Timestamp.valueOf(date.getLocalDate());
    }
}
