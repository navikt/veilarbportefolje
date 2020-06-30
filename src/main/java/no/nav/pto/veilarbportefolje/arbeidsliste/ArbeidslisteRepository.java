package no.nav.pto.veilarbportefolje.arbeidsliste;

import com.google.common.base.Supplier;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.sbl.sql.SqlUtils.*;

@Slf4j
@Repository
public class ArbeidslisteRepository {

    private static final String DELETE_FROM_ARBEIDSLISTE_SQL = "delete from arbeidsliste where aktoerid = :aktoerid";
    public static final String ARBEIDSLISTE = "ARBEIDSLISTE";

    private final JdbcTemplate db;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public ArbeidslisteRepository (JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }


    public Try<Arbeidsliste> retrieveArbeidsliste(AktoerId aktoerId) {
        return Try.of(
                () -> select(db, ARBEIDSLISTE, ArbeidslisteRepository::arbeidslisteMapper)
                        .column("*")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        );
    }

    public Try<AktoerId> insertArbeidsliste(ArbeidslisteDTO data) {
        return Try.of(
                () -> {

                    AktoerId aktoerId = Optional
                            .ofNullable(data.getAktoerId())
                            .orElseThrow(() -> new RuntimeException("Fant ikke aktør-ID"));

                    upsert(db, ARBEIDSLISTE)
                            .set("AKTOERID", aktoerId.toString())
                            .set("SIST_ENDRET_AV_VEILEDERIDENT", data.getVeilederId().toString())
                            .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                            .set("OVERSKRIFT", data.getOverskrift())
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .set("KATEGORI", data.getKategori().name())
                            .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                            .execute();
                    return data.getAktoerId();
                }
        ).onFailure(e -> log.warn("Kunne ikke inserte arbeidsliste til db", e));
    }


    public Try<AktoerId> updateArbeidsliste(ArbeidslisteDTO data) {
        return Try.of(
                () -> {
                    update(db, ARBEIDSLISTE)
                            .set("SIST_ENDRET_AV_VEILEDERIDENT", data.getVeilederId().toString())
                            .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                            .set("OVERSKRIFT", data.getOverskrift())
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .set("KATEGORI", data.getKategori().name())
                            .whereEquals("AKTOERID", data.getAktoerId().toString())
                            .execute();
                    return data.getAktoerId();
                }
        ).onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste i db", e));
    }

    public Try<AktoerId> deleteArbeidsliste(AktoerId aktoerID) {
        return Try.of(
                () -> {
                    delete(db, ARBEIDSLISTE)
                            .where(WhereClause.equals("AKTOERID", aktoerID.toString()))
                            .execute();
                    return aktoerID;
                }
        )
                .onSuccess((aktoerid) -> log.info("Arbeidsliste for aktoerid {} slettet", aktoerid.toString()))
                .onFailure(e -> log.warn("Kunne ikke slette arbeidsliste fra db", e));
    }

    //TODO SLETTE EN AV DISSE DELETE METODER???

    public Result<Integer> deleteArbeidslisteForAktoerid(AktoerId aktoerId) {
        Supplier<Integer> query = () -> {
            return namedParameterJdbcTemplate
                    .update(DELETE_FROM_ARBEIDSLISTE_SQL,
                            Collections.singletonMap("aktoerid", aktoerId.toString())
                    );
        };
        return Result.of(query);
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
