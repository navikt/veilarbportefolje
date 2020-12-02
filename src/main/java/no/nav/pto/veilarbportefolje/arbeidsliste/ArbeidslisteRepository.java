package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Optional;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.Table.ARBEIDSLISTE.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.sbl.sql.SqlUtils.*;

@Slf4j
@Repository
public class ArbeidslisteRepository {

    private static final String DELETE_FROM_ARBEIDSLISTE_SQL = "delete from arbeidsliste where aktoerid = :aktoerid";

    private final JdbcTemplate db;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public ArbeidslisteRepository(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    // TODO: endre til bruk av elastic
    public Optional<String> hentNavKontorForArbeidsliste(AktoerId aktoerId) {
        String navKontor = select(db, Table.ARBEIDSLISTE.TABLE_NAME, rs -> rs.getString(NAV_KONTOR_FOR_ARBEIDSLISTE))
                .column(NAV_KONTOR_FOR_ARBEIDSLISTE)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();

        return Optional.ofNullable(navKontor);
    }

    public Try<Arbeidsliste> retrieveArbeidslisteFromDb(AktoerId aktoerId) {
        return Try.of(
                () -> select(db, Table.ARBEIDSLISTE.TABLE_NAME, ArbeidslisteRepository::arbeidslisteMapper)
                        .column("*")
                        .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                        .execute()
        );
    }

    public Try<ArbeidslisteDTO> insertArbeidsliste(ArbeidslisteDTO dto) {
        return Try.of(
                () -> {
                    dto.setEndringstidspunkt(Timestamp.from(now()));
                    AktoerId aktoerId = Optional
                            .ofNullable(dto.getAktoerId())
                            .orElseThrow(() -> new RuntimeException("Fant ikke aktør-ID"));

                    upsert(db, TABLE_NAME)
                            .set(AKTOERID, aktoerId.toString())
                            .set(FNR, dto.getFnr().toString())
                            .set(SIST_ENDRET_AV_VEILEDERIDENT, dto.getVeilederId().toString())
                            .set(ENDRINGSTIDSPUNKT, dto.getEndringstidspunkt())
                            .set(OVERSKRIFT, dto.getOverskrift())
                            .set(KOMMENTAR, dto.getKommentar())
                            .set(FRIST, dto.getFrist())
                            .set(KATEGORI, dto.getKategori().name())
                            .set(NAV_KONTOR_FOR_ARBEIDSLISTE, dto.getNavKontorForArbeidsliste())
                            .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                            .execute();

                    return dto;
                }
        ).onFailure(e -> log.warn("Kunne ikke inserte arbeidsliste til db", e));
    }


    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        return Try.of(
                () -> {
                    data.setEndringstidspunkt(Timestamp.from(now()));
                    update(db, TABLE_NAME)
                            .set("SIST_ENDRET_AV_VEILEDERIDENT", data.getVeilederId().toString())
                            .set("ENDRINGSTIDSPUNKT", data.getEndringstidspunkt())
                            .set("OVERSKRIFT", data.getOverskrift())
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .set("KATEGORI", data.getKategori().name())
                            .whereEquals("AKTOERID", data.getAktoerId().toString())
                            .execute();
                    return data;
                }
        ).onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste i db", e));
    }

    //TODO SLETTE EN AV DISSE DELETE METODER???

    public Try<AktoerId> deleteArbeidsliste(AktoerId aktoerID) {
        return Try.of(
                () -> {
                    delete(db, TABLE_NAME)
                            .where(WhereClause.equals("AKTOERID", aktoerID.toString()))
                            .execute();
                    return aktoerID;
                }
        )
                .onSuccess((aktoerid) -> log.info("Arbeidsliste for aktoerid {} slettet", aktoerid.toString()))
                .onFailure(e -> log.warn("Kunne ikke slette arbeidsliste fra db", e));
    }

    public Integer deleteArbeidslisteForAktoerid(AktoerId aktoerId) {
            return namedParameterJdbcTemplate
                    .update(DELETE_FROM_ARBEIDSLISTE_SQL,
                            Collections.singletonMap("aktoerid", aktoerId.toString())
                    );
    }

    @SneakyThrows
    private static Arbeidsliste arbeidslisteMapper(ResultSet rs) {
        return new Arbeidsliste(
                VeilederId.of(rs.getString("SIST_ENDRET_AV_VEILEDERIDENT")),
                toZonedDateTime(rs.getTimestamp("ENDRINGSTIDSPUNKT")),
                rs.getString("OVERSKRIFT"),
                rs.getString("KOMMENTAR"),
                toZonedDateTime(rs.getTimestamp("FRIST")),
                Arbeidsliste.Kategori.valueOf(rs.getString("KATEGORI"))
        );
    }

}
