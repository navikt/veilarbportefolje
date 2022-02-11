package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.Tiltakkodeverk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV2 {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsert(TiltakInnhold innhold, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeFra(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeTil(), true);

        log.info("Lagrer tiltak: {}", innhold.getAktivitetid());

        if (skalOppdatereTiltakskodeVerk(innhold.getTiltakstype(), innhold.getTiltaksnavn())) {
            upsertTiltakKodeVerk(innhold);
        }
        db.update("""
                        INSERT INTO brukertiltak
                        (aktivitetid, personid, aktoerid, tiltakskode, fradato, tildato) VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (aktivitetid) DO UPDATE SET (personid, aktoerid, tiltakskode, fradato, tildato)
                        = (excluded.personid, excluded.aktoerid, excluded.tiltakskode, excluded.fradato, excluded.tildato)
                        """,
                innhold.getAktivitetid(),
                String.valueOf(innhold.getPersonId()), aktorId.get(), innhold.getTiltakstype(), fraDato, tilDato
                );
    }

    public void delete(String tiltakId) {
        log.info("Sletter tiltak: {}", tiltakId);
        db.update("DELETE FROM brukertiltak WHERE aktivitetid = ?", tiltakId);
    }

    public EnhetTiltak hentTiltakPaEnhet(EnhetId enhetId) {
        final String hentTiltakPaEnhetSql = """
                SELECT * FROM tiltakkodeverket WHERE
                kode IN (SELECT DISTINCT tiltakskode FROM brukertiltak BT
                INNER JOIN oppfolgingsbruker_arena OP ON BT.aktoerid = OP.aktoerid
                WHERE OP.nav_kontor=?)
                """;

        return new EnhetTiltak().setTiltak(
                db.queryForList(hentTiltakPaEnhetSql, enhetId.get())
                        .stream().map(this::mapTilTiltak)
                        .collect(toMap(Tiltakkodeverk::getKode, Tiltakkodeverk::getVerdi))
        );
    }

    public Optional<String> hentVerdiITiltakskodeVerk(String kode) {
        String sql = "SELECT verdi FROM tiltakkodeverket WHERE kode = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, String.class, kode))
        );
    }

    private void upsertTiltakKodeVerk(TiltakInnhold innhold) {
        db.update("""
                        INSERT INTO tiltakkodeverket (kode, verdi) VALUES (?, ?)
                        ON CONFLICT (kode) DO UPDATE SET verdi = excluded.verdi
                        """,
                innhold.getTiltakstype(), innhold.getTiltaksnavn()
        );
    }

    private boolean skalOppdatereTiltakskodeVerk(String tiltaksKode, String verdiFraKafka) {
        Optional<String> verdiITiltakskodeVerk = hentVerdiITiltakskodeVerk(tiltaksKode);
        return verdiITiltakskodeVerk.map(lagretVerdi -> !lagretVerdi.equals(verdiFraKafka)).orElse(true);
    }

    @SneakyThrows
    private Tiltakkodeverk mapTilTiltak(Map<String, Object> rs) {
        return Tiltakkodeverk.of((String) rs.get(PostgresTable.TILTAKKODEVERK.KODE), (String) rs.get(PostgresTable.TILTAKKODEVERK.VERDI));
    }
}
