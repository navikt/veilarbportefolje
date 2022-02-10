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
        mapBulk(brukere, false);
    }

    public void mapBulk(List<OppfolgingsBruker> brukere, boolean medDiffLogging) {
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
                                entity -> flettInnPostgresData(entity, bruker, medDiffLogging),
                                () -> log.error("Fant ikke aktoer i postgres: {}", bruker.getAktoer_id()
                                )
                        )
        );
    }

    private void flettInnPostgresData(PostgresAktorIdEntity postgresAktorIdEntity, OppfolgingsBruker bruker, boolean medDiffLogging) {
        if(medDiffLogging){
            loggDiff(postgresAktorIdEntity, bruker);
        }
        bruker.setBrukers_situasjon(postgresAktorIdEntity.getBrukersSituasjon());
        bruker.setProfilering_resultat(postgresAktorIdEntity.getProfileringResultat());
        bruker.setUtdanning(postgresAktorIdEntity.getUtdanning());
        bruker.setUtdanning_bestatt(postgresAktorIdEntity.getUtdanningBestatt());
        bruker.setUtdanning_godkjent(postgresAktorIdEntity.getUtdanningGodkjent());
    }

    private void loggDiff(PostgresAktorIdEntity postgresAktorIdEntity, OppfolgingsBruker bruker){
        if (isDifferent(bruker.getBrukers_situasjon(), postgresAktorIdEntity.getBrukersSituasjon())) {
            log.warn("postgres Opensearch: Situsjon feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getProfilering_resultat(), postgresAktorIdEntity.getProfileringResultat())) {
            log.warn("postgres Opensearch: Profilering feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning(), postgresAktorIdEntity.getUtdanning())) {
            log.warn("postgres Opensearch: Utdanning feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning_bestatt(), postgresAktorIdEntity.getUtdanningBestatt())) {
            log.warn("postgres Opensearch: Utdanning bestått feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning_godkjent(), postgresAktorIdEntity.getUtdanningGodkjent())) {
            log.warn("postgres Opensearch: Utdanning godskjent feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isHar_delt_cv() != postgresAktorIdEntity.getHarDeltCv()) {
            log.info("postgres Opensearch: isHar_delt_cv feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isCv_eksistere() != postgresAktorIdEntity.getCvEksistere()) {
            log.info("postgres Opensearch: isCv_eksistere feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isOppfolging() != postgresAktorIdEntity.getOppfolging()) {
            log.warn("postgres Opensearch: isOppfolging feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isNy_for_veileder() != postgresAktorIdEntity.getNyForVeileder()) {
            log.warn("postgres Opensearch: isNy_for_veileder feil bruker: {}", bruker.getAktoer_id());
        }
        if ((bruker.getManuell_bruker() != null && bruker.getManuell_bruker().equals("MANUELL")) != postgresAktorIdEntity.getManuellBruker()) {
            log.warn("postgres Opensearch: getManuell_bruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getOppfolging_startdato(), postgresAktorIdEntity.getOppfolgingStartdato())) {
            log.warn("postgres Opensearch: getOppfolging_startdato feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfrabruker(), postgresAktorIdEntity.getVenterpasvarfrabruker())) {
            log.info("postgres Opensearch: Venterpasvarfrabruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfranav(), postgresAktorIdEntity.getVenterpasvarfranav())) {
            log.info("postgres Opensearch: getVenterpasvarfranav feil bruker: {}", bruker.getAktoer_id());
        }
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

        postgresAktorIdData.setBrukersSituasjon(rs.getString(BRUKERS_SITUASJON));
        postgresAktorIdData.setProfileringResultat(rs.getString(PROFILERING_RESULTAT));
        postgresAktorIdData.setUtdanning(rs.getString(UTDANNING));
        postgresAktorIdData.setUtdanningBestatt(rs.getString(UTDANNING_BESTATT));
        postgresAktorIdData.setUtdanningGodkjent(rs.getString(UTDANNING_GODKJENT));

        postgresAktorIdData.setHarDeltCv(rs.getBoolean(HAR_DELT_CV));
        postgresAktorIdData.setCvEksistere(rs.getBoolean(CV_EKSISTERER));

        postgresAktorIdData.setOppfolging(rs.getBoolean(OPPFOLGING));
        postgresAktorIdData.setNyForVeileder(rs.getBoolean(NY_FOR_VEILEDER));
        postgresAktorIdData.setManuellBruker(rs.getBoolean(MANUELL));
        postgresAktorIdData.setOppfolgingStartdato(toIsoUTC(rs.getTimestamp(STARTDATO)));

        postgresAktorIdData.setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp(VENTER_PA_BRUKER)));
        postgresAktorIdData.setVenterpasvarfranav(toIsoUTC(rs.getTimestamp(VENTER_PA_NAV)));

        postgresAktorIdData.setVedtak14AStatus(rs.getString(VEDTAKSTATUS));
        postgresAktorIdData.setVedtak14AStatusEndret(toIsoUTC(rs.getTimestamp(VEDTAKSTATUS_ENDRET_TIDSPUNKT)));
        postgresAktorIdData.setAnsvarligVeilederFor14AVedtak(rs.getString(VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN));

        String arbeidslisteTidspunkt = toIsoUTC(rs.getTimestamp(ARB_ENDRINGSTIDSPUNKT));
        if (arbeidslisteTidspunkt != null) {
            postgresAktorIdData.setArbeidslisteAktiv(true);
            postgresAktorIdData.setArbeidslisteEndringstidspunkt(arbeidslisteTidspunkt);
            postgresAktorIdData.setArbeidslisteFrist(toIsoUTC(rs.getTimestamp(ARB_FRIST)));
            postgresAktorIdData.setArbeidslisteKategori(rs.getString(ARB_KATEGORI));
            postgresAktorIdData.setArbeidslisteSistEndretAvVeilederid(rs.getString(ARB_SIST_ENDRET_AV_VEILEDERIDENT));
            String overskrift = rs.getString(ARB_OVERSKRIFT);

            postgresAktorIdData.setArbeidslisteTittelLengde(
                    Optional.ofNullable(overskrift)
                            .map(String::length)
                            .orElse(0));
            postgresAktorIdData.setArbeidslisteTittelSortering(
                    Optional.ofNullable(overskrift)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s.substring(0, Math.min(2, s.length())))
                            .orElse(""));
        }
        return postgresAktorIdData;
    }
}
