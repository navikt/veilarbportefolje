package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.AKTORID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.ANTALLPERMITTERINGSUKER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.ANTALLUKERIGJEN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.ANTALLUKERIGJENUNNTAK;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.PERSONID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.RETTIGHETSTYPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.SAKSID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.SAKSTYPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.STARTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.UTLOPSDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSESVEDTAK.YTELSESTYPE;

@Repository
@RequiredArgsConstructor
public class YtelsesRepositoryV2 {
    private final JdbcTemplate db;

    public void upsert(AktorId aktorId, TypeKafkaYtelse type, YtelsesInnhold innhold) {
        LocalDateTime startdato = getLocalDateTimeOrNull(innhold.getFraOgMedDato(), false);
        LocalDateTime utlopsdato = getLocalDateTimeOrNull(innhold.getTilOgMedDato(), true);

        db.update("""
                        INSERT INTO YTELSESVEDTAK
                        (VEDTAKSID, AKTORID, PERSONID, YTELSESTYPE, SAKSID, SAKSTYPEKODE, RETTIGHETSTYPEKODE,
                        STARTDATO, UTLOPSDATO, ANTALLUKERIGJEN, ANTALLPERMITTERINGSUKER, ANTALLUKERIGJENUNNTAK)
                        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (VEDTAKSID)
                        DO UPDATE SET (AKTORID, PERSONID, YTELSESTYPE, SAKSID, SAKSTYPEKODE, RETTIGHETSTYPEKODE,
                        STARTDATO, UTLOPSDATO, ANTALLUKERIGJEN, ANTALLPERMITTERINGSUKER, ANTALLUKERIGJENUNNTAK) =
                        (EXCLUDED.AKTORID, EXCLUDED.PERSONID, EXCLUDED.YTELSESTYPE, EXCLUDED.SAKSID, EXCLUDED.SAKSTYPEKODE, EXCLUDED.RETTIGHETSTYPEKODE,
                        EXCLUDED.STARTDATO, EXCLUDED.UTLOPSDATO, EXCLUDED.ANTALLUKERIGJEN, EXCLUDED.ANTALLPERMITTERINGSUKER, EXCLUDED.ANTALLUKERIGJENUNNTAK)
                        """,
                innhold.getVedtakId(), aktorId.get(), innhold.getPersonId(),
                type.toString(), innhold.getSaksId(), innhold.getSakstypeKode(), innhold.getRettighetstypeKode(), startdato, utlopsdato,
                innhold.getAntallUkerIgjen(), innhold.getAntallUkerIgjenUnderPermittering(), innhold.getAntallDagerIgjenUnntak());
    }

    public List<YtelseDAO> getYtelser(AktorId aktorId) {
        if (aktorId == null) {
            return new ArrayList<>();
        }

        final String hentRadSql = "SELECT * FROM YTELSESVEDTAK WHERE AKTORID = ?";

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
        db.update("DELETE FROM YTELSESVEDTAK WHERE VEDTAKSID = ?", vedtaksId);
    }

    public List<AktorId> hentBrukereMedYtelserSomStarterIDag() {
        final String brukereSomStarterIDag = "SELECT distinct AKTORID FROM YTELSESVEDTAK WHERE date_trunc('day', STARTDATO) = date_trunc('day',current_timestamp)";

        return db.queryForList(brukereSomStarterIDag, AktorId.class);
    }
}
