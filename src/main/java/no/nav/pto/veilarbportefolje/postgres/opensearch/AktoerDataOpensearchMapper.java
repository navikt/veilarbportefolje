package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseUtils.konverterDagerTilUker;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.AAPMAXTIDUKE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.AAPUNNTAKDAGERIGJEN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_ENDRINGSTIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_FRIST;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_KATEGORI;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_OVERSKRIFT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.ARB_SIST_ENDRET_AV_VEILEDERIDENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.BRUKERS_SITUASJON;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.CV_EKSISTERER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.DAGPUTLOPUKE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.HAR_DELT_CV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.MANUELL;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.NY_FOR_VEILEDER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.OPPFOLGING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.PERMUTLOPUKE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.PROFILERING_RESULTAT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.STARTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.UTDANNING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.UTDANNING_BESTATT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.UTDANNING_GODKJENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEDTAKSTATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEDTAKSTATUS_ENDRET_TIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VEILEDERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VENTER_PA_BRUKER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.VENTER_PA_NAV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.YTELSE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.YTELSE_UTLOPSDATO;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktoerDataOpensearchMapper {
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate db;

    public HashMap<AktorId, PostgresAktorIdEntity> hentAktoerData(List<AktorId> brukere) {
        String aktoerIder = brukere.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        return Optional.ofNullable(
                        db.query("SELECT * FROM aktorid_indeksert_data WHERE aktoerid = ANY (:ids::varchar[])",
                                new MapSqlParameterSource("ids", aktoerIder),
                                (ResultSet rs) -> {
                                    HashMap<AktorId, PostgresAktorIdEntity> results = new HashMap<>();
                                    while (rs.next()) {
                                        results.put(AktorId.of(rs.getString(AKTOERID)), mapTilEntity(rs));
                                    }
                                    return results;
                                }))
                .orElse(new HashMap<>());
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
        postgresAktorIdData.setVeileder(rs.getString(VEILEDERID));
        postgresAktorIdData.setManuellBruker(rs.getBoolean(MANUELL));
        postgresAktorIdData.setOppfolgingStartdato(toIsoUTC(rs.getTimestamp(STARTDATO)));

        postgresAktorIdData.setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp(VENTER_PA_BRUKER)));
        postgresAktorIdData.setVenterpasvarfranav(toIsoUTC(rs.getTimestamp(VENTER_PA_NAV)));

        postgresAktorIdData.setVedtak14AStatus(rs.getString(VEDTAKSTATUS));
        postgresAktorIdData.setVedtak14AStatusEndret(toIsoUTC(rs.getTimestamp(VEDTAKSTATUS_ENDRET_TIDSPUNKT)));
        postgresAktorIdData.setAnsvarligVeilederFor14AVedtak(rs.getString(VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN));

        postgresAktorIdData.setYtelse(rs.getString(YTELSE));
        postgresAktorIdData.setYtelseUtlopsdato(toIsoUTC(rs.getTimestamp(YTELSE_UTLOPSDATO)));
        postgresAktorIdData.setDagputlopuke(rs.getObject(DAGPUTLOPUKE, Integer.class));
        postgresAktorIdData.setPermutlopuke(rs.getObject(PERMUTLOPUKE, Integer.class));
        postgresAktorIdData.setAapmaxtiduke(rs.getObject(AAPMAXTIDUKE, Integer.class));
        postgresAktorIdData.setAapunntakukerigjen(konverterDagerTilUker(rs.getObject(AAPUNNTAKDAGERIGJEN, Integer.class)));

        String arbeidslisteTidspunkt = toIsoUTC(rs.getTimestamp(ARB_ENDRINGSTIDSPUNKT));
        if (arbeidslisteTidspunkt != null) {
            postgresAktorIdData.setArbeidslisteAktiv(true);
            postgresAktorIdData.setArbeidslisteEndringstidspunkt(arbeidslisteTidspunkt);
            postgresAktorIdData.setArbeidslisteFrist(Optional.ofNullable(toIsoUTC(rs.getTimestamp(ARB_FRIST))).orElse(getFarInTheFutureDate()));
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
