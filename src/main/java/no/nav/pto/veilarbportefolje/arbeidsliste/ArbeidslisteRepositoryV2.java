package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSLISTE.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArbeidslisteRepositoryV2 {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;


    public Optional<String> hentNavKontorForArbeidsliste(AktorId aktorId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s=? ", NAV_KONTOR_FOR_ARBEIDSLISTE, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getString(NAV_KONTOR_FOR_ARBEIDSLISTE), aktorId.get()))
        );
    }

    public Try<Arbeidsliste> retrieveArbeidsliste(Fnr bruker) {
        String sql = """
                    SELECT a.*, f.verdi FROM arbeidsliste a
                    INNER JOIN aktive_identer ai on ai.aktorid = a.aktoerid
                    LEFT JOIN fargekategori f on f.fnr = ai.fnr
                    WHERE ai.fnr = ?
                """;
        return Try.of(
                () -> queryForObjectOrNull(
                        () -> db.queryForObject(sql, ArbeidslisteMapper::arbeidslisteMapper, bruker.get())
                )
        );
    }

    public List<Arbeidsliste> hentArbeidslisteForVeilederPaEnhet(EnhetId enhet, VeilederId veilederident) {
        return dbReadOnly.queryForList("""
                                SELECT a.*, f.verdi FROM arbeidsliste a
                                INNER JOIN oppfolging_data o ON a.aktoerid = o.aktoerid
                                INNER JOIN aktive_identer ai on ai.aktorid = a.aktoerid
                                INNER JOIN oppfolgingsbruker_arena_v2 ob on ai.fnr = ob.fodselsnr
                                LEFT JOIN fargekategori f on f.fnr = ai.fnr
                                WHERE ob.nav_kontor = ?
                                AND o.veilederid = ?""",
                        enhet.get(),
                        veilederident.getValue()
                )
                .stream()
                .map(ArbeidslisteMapper::arbeidslisteMapper)
                .toList();
    }

    @Transactional
    public Try<ArbeidslisteDTO> insertArbeidsliste(ArbeidslisteDTO dto) {
        return Try.of(
                () -> {
                    dto.setEndringstidspunkt(Timestamp.from(now()));
                    AktorId aktoerId = Optional
                            .ofNullable(dto.getAktorId())
                            .orElseThrow(() -> new RuntimeException("Fant ikke aktør-ID"));
                    dto.setAktorId(aktoerId);

                    upsert(aktoerId.get(), dto);
                    return dto;
                }
        ).onFailure(e -> secureLog.warn("Kunne ikke inserte arbeidsliste til db", e));
    }

    @Transactional
    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        final String updateSql = String.format(
                "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?",
                TABLE_NAME, SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT, OVERSKRIFT,
                KOMMENTAR, FRIST, KATEGORI, AKTOERID
        );

        Timestamp endringsTidspunkt = Timestamp.from(now());
        return Try.of(
                () -> {
                    int arbeidslisteRows = db.update(updateSql, data.getVeilederId().getValue(), endringsTidspunkt, data.getOverskrift(),
                            data.getKommentar(), data.getFrist(), null, data.getAktorId().get());

                    int fargekategoriRows = db.update("""
                                    INSERT INTO FARGEKATEGORI (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                                    VALUES(?,?,?,?,?) ON CONFLICT (FNR) DO UPDATE SET
                                    (VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT) = (excluded.VERDI, excluded.SIST_ENDRET, excluded.SIST_ENDRET_AV_VEILEDERIDENT)
                                    """,
                            UUID.randomUUID(), data.getFnr().get(), ArbeidslisteMapper.mapTilFargekategoriVerdi(data.getKategori()), endringsTidspunkt, data.getVeilederId().getValue());

                    secureLog.info("Oppdaterte arbeidsliste pa bruker {}, rader: {}", data.getAktorId().get(), arbeidslisteRows + fargekategoriRows);
                    return data.setEndringstidspunkt(endringsTidspunkt);
                }
        ).onFailure(e -> secureLog.warn("Kunne ikke oppdatere arbeidsliste i db", e));
    }

    @Transactional
    public int slettArbeidsliste(AktorId aktoerId, Optional<Fnr> maybeFnr) {
        if (aktoerId == null) {
            return 0;
        }
        secureLog.info("Sletter arbeidsliste pa bruker: {}", aktoerId);

        int oppdaterteRaderArbeidsliste = db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());

        int oppdaterteRaderFargekategori = 0;
        if (maybeFnr.isPresent()) {
            oppdaterteRaderFargekategori = db.update("DELETE FROM FARGEKATEGORI WHERE FNR = ?", maybeFnr.get().get());
        } else {
            secureLog.warn("Kunne ikke slette fargekategori for bruker med AktørID {}. Årsak fødselsnummer-parameter var tom.", aktoerId.get());
        }

        if (oppdaterteRaderArbeidsliste > 1 || oppdaterteRaderFargekategori > 1) {
            secureLog.error(String.format(
                    "Fant flere rader i ARBEIDSLISTE/FARGEKATEGORI-tabell. Fnr: %s - antall rader i FARGEKATEGORI: %s. AktørID: %s - antall rader i ARBEIDSLISTE: %s.",
                    maybeFnr.orElse(Fnr.of("")),
                    oppdaterteRaderFargekategori,
                    aktoerId.get(),
                    oppdaterteRaderArbeidsliste
            ));

            throw new SlettArbeidslisteException("Fant flere rader i ARBEIDSLISTE/FARGEKATEGORI-tabell for bruker.");
        }

        return (oppdaterteRaderArbeidsliste + oppdaterteRaderFargekategori);
    }

    @Transactional
    public int slettArbeidslisteUtenFargekategori(AktorId aktoerId) {
        if (aktoerId == null) {
            return 0;
        }
        secureLog.info("Sletter arbeidsliste pa bruker: {}", aktoerId);

        int oppdaterteRaderArbeidsliste = db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());

        if (oppdaterteRaderArbeidsliste > 1) {
            secureLog.error(String.format(
                    "Fant flere rader i ARBEIDSLISTE-tabell. AktørID: %s - antall rader i ARBEIDSLISTE: %s.",
                    aktoerId.get(),
                    oppdaterteRaderArbeidsliste
            ));

            throw new SlettArbeidslisteException("Fant flere rader i ARBEIDSLISTE-tabell for bruker.");
        }

        return oppdaterteRaderArbeidsliste;
    }

    private void upsert(String aktoerId, ArbeidslisteDTO dto) {
        secureLog.info("Upsert arbeidsliste pa bruker: {}", aktoerId);

        int oppdaterteRaderIArbeidsliste = db.update("""
                        INSERT INTO ARBEIDSLISTE (AKTOERID, SIST_ENDRET_AV_VEILEDERIDENT , ENDRINGSTIDSPUNKT,
                        OVERSKRIFT, KOMMENTAR, FRIST , KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE)
                        VALUES(?,?,?,?,?,?,?,?) ON CONFLICT (AKTOERID) DO UPDATE SET
                        (SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT, OVERSKRIFT, KOMMENTAR , FRIST , KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE) =
                        (excluded.SIST_ENDRET_AV_VEILEDERIDENT, excluded.ENDRINGSTIDSPUNKT, excluded.OVERSKRIFT, excluded.KOMMENTAR , excluded.FRIST , excluded.KATEGORI, excluded.NAV_KONTOR_FOR_ARBEIDSLISTE)
                        """,
                aktoerId, dto.getVeilederId().getValue(), dto.getEndringstidspunkt(), dto.getOverskrift(), dto.getKommentar(), dto.getFrist(), null, dto.getNavKontorForArbeidsliste());

        int oppdaterteRaderIFargekategori = db.update("""
                        INSERT INTO FARGEKATEGORI (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                        VALUES(?,?,?,?,?) ON CONFLICT (FNR) DO UPDATE SET
                        (VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT) = (excluded.VERDI, excluded.SIST_ENDRET, excluded.SIST_ENDRET_AV_VEILEDERIDENT)
                        """,
                UUID.randomUUID(), dto.getFnr().get(), ArbeidslisteMapper.mapTilFargekategoriVerdi(dto.getKategori()), dto.getEndringstidspunkt(), dto.getVeilederId().getValue());

        if (oppdaterteRaderIArbeidsliste != oppdaterteRaderIFargekategori) {
            log.warn("Oppdaterte ulikt antall rader i henholdsvis ARBEIDSLISTE og FARGEKATEGORI. " +
                    "I utgangspunktet forventet vi at likt antall ble oppdatert. Sjekk opp i hva som har skjedd og tilpass " +
                    "loggmelding dersom det eventuelt viser seg at dette er forventet."
            );
        }
    }
}
