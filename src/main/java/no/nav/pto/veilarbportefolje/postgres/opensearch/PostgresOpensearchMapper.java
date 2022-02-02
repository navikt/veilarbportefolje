package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_ENDRINGSTIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_FRIST;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_KATEGORI;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_OVERSKRIFT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_SIST_ENDRET_AV_VEILEDERIDENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.BRUKERS_SITUASJON;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.CV_EKSISTERER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.HAR_DELT_CV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.MANUELL;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.NY_FOR_VEILEDER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.OPPFOLGING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.PROFILERING_RESULTAT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.STARTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.UTDANNING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.UTDANNING_BESTATT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.UTDANNING_GODKJENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEDTAKSTATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEDTAKSTATUS_ENDRET_TIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VENTER_PA_BRUKER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VENTER_PA_NAV;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresOpensearchMapper {
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate db;

    public void mapBulk(List<OppfolgingsBruker> brukere) {
        String aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(Collectors.joining(",", "{", "}"));
        HashMap<String, PostgresAktorIdEntity> resultMap = Optional.ofNullable(
                        db.query("SELECT * FROM aktorid_indeksert_data WHERE aktoerid = ANY (:ids::varchar[])",
                                new MapSqlParameterSource("ids", aktoerIder),
                                (ResultSet rs) -> {
                                    HashMap<String, PostgresAktorIdEntity> results = new HashMap<>();
                                    while (rs.next()) {
                                        results.put(rs.getString(AKTOERID), mapTilEntity(rs));
                                    }
                                    return results;
                                }))
                .orElse(new HashMap<>());

        brukere.forEach(bruker ->
                Optional.ofNullable(resultMap.get(bruker.getAktoer_id()))
                        .ifPresentOrElse(
                                entity -> flettInnPostgresData(entity, bruker),
                                () -> log.warn("Fant ikke aktoer i postgres: {}", bruker.getAktoer_id()
                                )
                        )
        );
    }

    private void flettInnPostgresData(PostgresAktorIdEntity postgresAktorIdEntity, OppfolgingsBruker bruker) {
        if (isDifferent(bruker.getBrukers_situasjon(), postgresAktorIdEntity.getBrukers_situasjon())) {
            log.warn("postgres Opensearch: Situsjon feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getProfilering_resultat(), postgresAktorIdEntity.getProfilering_resultat())) {
            log.warn("postgres Opensearch: Profilering feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning(), postgresAktorIdEntity.getUtdanning())) {
            log.warn("postgres Opensearch: Utdanning feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning_bestatt(), postgresAktorIdEntity.getUtdanning_bestatt())) {
            log.warn("postgres Opensearch: Utdanning bestått feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning_godkjent(), postgresAktorIdEntity.getUtdanning_godkjent())) {
            log.warn("postgres Opensearch: Utdanning godskjent feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isHar_delt_cv() != postgresAktorIdEntity.getHar_delt_cv()) {
            log.info("postgres Opensearch: isHar_delt_cv feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isCv_eksistere() != postgresAktorIdEntity.getCv_eksistere()) {
            log.info("postgres Opensearch: isCv_eksistere feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isOppfolging() != postgresAktorIdEntity.getOppfolging()) {
            log.warn("postgres Opensearch: isOppfolging feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isNy_for_veileder() != postgresAktorIdEntity.getNy_for_veileder()) {
            log.warn("postgres Opensearch: isNy_for_veileder feil bruker: {}", bruker.getAktoer_id());
        }
        if ((bruker.getManuell_bruker() != null && bruker.getManuell_bruker().equals("MANUELL")) != postgresAktorIdEntity.getManuell_bruker()) {
            log.warn("postgres Opensearch: getManuell_bruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getOppfolging_startdato(), postgresAktorIdEntity.getOppfolging_startdato())) {
            log.warn("postgres Opensearch: getOppfolging_startdato feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfrabruker(), postgresAktorIdEntity.getVenterpasvarfrabruker())) {
            log.info("postgres Opensearch: Venterpasvarfrabruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfranav(), postgresAktorIdEntity.getVenterpasvarfranav())) {
            log.info("postgres Opensearch: getVenterpasvarfranav feil bruker: {}", bruker.getAktoer_id());
        }

        bruker.setBrukers_situasjon(postgresAktorIdEntity.getBrukers_situasjon());
        bruker.setProfilering_resultat(postgresAktorIdEntity.getProfilering_resultat());
        bruker.setUtdanning(postgresAktorIdEntity.getUtdanning());
        bruker.setUtdanning_bestatt(postgresAktorIdEntity.getUtdanning_bestatt());
        bruker.setUtdanning_godkjent(postgresAktorIdEntity.getUtdanning_godkjent());
    }

    private boolean isDifferent(Object o, Object other) {
        if (o == null && other == null) {
            return false;
        } else if (o == null || other == null) {
            return true;
        }
        return !o.equals(other);
    }

    @SneakyThrows
    private PostgresAktorIdEntity mapTilEntity(ResultSet rs) {
        PostgresAktorIdEntity postgresAktorIdData = new PostgresAktorIdEntity();
        postgresAktorIdData.setAktoerId(rs.getString(AKTOERID));

        postgresAktorIdData.setBrukers_situasjon(rs.getString(BRUKERS_SITUASJON));
        postgresAktorIdData.setProfilering_resultat(rs.getString(PROFILERING_RESULTAT));
        postgresAktorIdData.setUtdanning(rs.getString(UTDANNING));
        postgresAktorIdData.setUtdanning_bestatt(rs.getString(UTDANNING_BESTATT));
        postgresAktorIdData.setUtdanning_godkjent(rs.getString(UTDANNING_GODKJENT));

        postgresAktorIdData.setHar_delt_cv(rs.getBoolean(HAR_DELT_CV));
        postgresAktorIdData.setCv_eksistere(rs.getBoolean(CV_EKSISTERER));

        postgresAktorIdData.setOppfolging(rs.getBoolean(OPPFOLGING));
        postgresAktorIdData.setNy_for_veileder(rs.getBoolean(NY_FOR_VEILEDER));
        postgresAktorIdData.setManuell_bruker(rs.getBoolean(MANUELL));
        postgresAktorIdData.setOppfolging_startdato(toIsoUTC(rs.getTimestamp(STARTDATO)));

        postgresAktorIdData.setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp(VENTER_PA_BRUKER)));
        postgresAktorIdData.setVenterpasvarfranav(toIsoUTC(rs.getTimestamp(VENTER_PA_NAV)));

        postgresAktorIdData.setVedtak_status(rs.getString(VEDTAKSTATUS));
        postgresAktorIdData.setVedtak_status_endret(toIsoUTC(rs.getTimestamp(VEDTAKSTATUS_ENDRET_TIDSPUNKT)));
        postgresAktorIdData.setAnsvarlig_veileder_for_vedtak(rs.getString(VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN));

        String arbeidslisteTidspunkt = toIsoUTC(rs.getTimestamp(ARB_ENDRINGSTIDSPUNKT));
        if (arbeidslisteTidspunkt != null) {
            postgresAktorIdData.setArbeidsliste_aktiv(true);
            postgresAktorIdData.setArbeidsliste_endringstidspunkt(arbeidslisteTidspunkt);
            postgresAktorIdData.setArbeidsliste_frist(toIsoUTC(rs.getTimestamp(ARB_FRIST)));
            postgresAktorIdData.setArbeidsliste_kategori(rs.getString(ARB_KATEGORI));
            postgresAktorIdData.setArbeidsliste_sist_endret_av_veilederid(rs.getString(ARB_SIST_ENDRET_AV_VEILEDERIDENT));
            String overskrift = rs.getString(ARB_OVERSKRIFT);

            postgresAktorIdData.setArbeidsliste_tittel_lengde(
                    Optional.ofNullable(overskrift)
                            .map(String::length)
                            .orElse(0));
            postgresAktorIdData.setArbeidsliste_tittel_sortering(
                    Optional.ofNullable(overskrift)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s.substring(0, Math.min(2, s.length())))
                            .orElse(""));
        }
        return postgresAktorIdData;
    }
}
