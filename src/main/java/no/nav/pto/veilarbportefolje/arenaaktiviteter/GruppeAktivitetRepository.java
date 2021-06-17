package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetInnhold;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.arenaaktiviteter.ArenaAktivitetUtils.getDateOrNull;
import static no.nav.pto.veilarbportefolje.database.Table.GRUPPE_AKTIVITER.*;


@Repository
@Transactional
@Slf4j
@RequiredArgsConstructor
public class GruppeAktivitetRepository {

    private final JdbcTemplate db;

    public boolean upsertGruppeAktivitet(GruppeAktivitetInnhold gruppeAktivitet , AktorId aktorId, boolean aktiv) {
        // Fra dato kan ha verdien null, det tilsier at aktiviteten varer en hel dag
        ZonedDateTime tilDato = getDateOrNull(gruppeAktivitet.getAktivitetperiodeTil(), true);
        ZonedDateTime fraDato = gruppeAktivitet.getAktivitetperiodeFra() == null ? tilDato.minusDays(1) : getDateOrNull(gruppeAktivitet.getAktivitetperiodeFra());
        String aktivChar = aktiv ? "J" : "N";

        return SqlUtils.upsert(db,  TABLE_NAME)
                .set(MOTEPLAN_ID, gruppeAktivitet.getMoteplanId())
                .set(VEILEDNINGDELTAKER_ID, gruppeAktivitet.getVeiledningdeltakerId())
                .set(AKTOERID, aktorId.get())
                .set(MOTEPLAN_STARTDATO, fraDato)
                .set(MOTEPLAN_SLUTTDATO, tilDato)
                .set(HENDELSE_ID, gruppeAktivitet.getHendelseId())
                .set(AKTIV, aktivChar)
                .where(
                        WhereClause.equals(MOTEPLAN_ID, gruppeAktivitet.getMoteplanId())
                        .and(WhereClause.equals(VEILEDNINGDELTAKER_ID, gruppeAktivitet.getVeiledningdeltakerId()))
                )
                .execute();
    }

    public Long retrieveHendelse(GruppeAktivitetInnhold innhold) {
        return null;
    }
}
