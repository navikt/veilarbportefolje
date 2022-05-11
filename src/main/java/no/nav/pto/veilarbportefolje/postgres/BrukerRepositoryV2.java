package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseUtils.konverterDagerTilUker;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukArenaSomBackup;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukNOMSkjerming;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukPDLBrukerdata;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.AAPMAXTIDUKE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.AAPUNNTAKDAGERIGJEN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ARB_ENDRINGSTIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ARB_FRIST;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ARB_KATEGORI;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ARB_OVERSKRIFT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ARB_SIST_ENDRET_AV_VEILEDERIDENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.BRUKERS_SITUASJON;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.CV_EKSISTERER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.DAGPUTLOPUKE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.DISKRESJONSKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ER_DOED;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ER_SKJERMET;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ETTERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.FODSELSNR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.FORMIDLINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.FORNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.HAR_DELT_CV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.HOVEDMAALKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.ISERV_FRA_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.KVALIFISERINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.MANUELL;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.NAV_KONTOR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.NY_FOR_VEILEDER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.OPPFOLGING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.PERMUTLOPUKE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.PROFILERING_RESULTAT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.RETTIGHETSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.SIKKERHETSTILTAK_TYPE_KODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.SPERRET_ANSATT_ARENA;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.STARTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.UTDANNING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.UTDANNING_BESTATT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.UTDANNING_GODKJENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.VEDTAKSTATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.VEDTAKSTATUS_ENDRET_TIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.VEILEDERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.VENTER_PA_BRUKER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.VENTER_PA_NAV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.YTELSE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.YTELSE_UTLOPSDATO;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.FodselsnummerUtils.lagFodselsdato;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BrukerRepositoryV2 {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate db;
    private final UnleashService unleashService;

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds) {
        return hentOppfolgingsBrukere(aktorIds, false);
    }

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds, boolean logdiff) {
        List<OppfolgingsBruker> result = new ArrayList<>();

        var params = aktorIds.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        return db.query("""
                        select OD.AKTOERID, OD.OPPFOLGING, ob.*,
                               ns.er_skjermet, ai.fnr, bd.foedselsdato, bd.fornavn as fornavn_pdl,
                               bd.etternavn as etternavn_pdl, bd.er_doed as er_doed_pdl, bd.kjoenn,
                               OD.STARTDATO, OD.NY_FOR_VEILEDER, OD.VEILEDERID, OD.MANUELL,  D.VENTER_PA_BRUKER,  D.VENTER_PA_NAV,
                               V.VEDTAKSTATUS, BP.PROFILERING_RESULTAT, CV.HAR_DELT_CV, CV.CV_EKSISTERER, BR.BRUKERS_SITUASJON,
                               BR.UTDANNING, BR.UTDANNING_BESTATT, BR.UTDANNING_GODKJENT, YB.YTELSE, YB.AAPMAXTIDUKE, YB.AAPUNNTAKDAGERIGJEN,
                               YB.DAGPUTLOPUKE, YB.PERMUTLOPUKE, YB.UTLOPSDATO as YTELSE_UTLOPSDATO,
                               V.ANSVARLIG_VEILDERNAVN          as VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN,
                               V.ENDRET_TIDSPUNKT               as VEDTAKSTATUS_ENDRET_TIDSPUNKT,
                               ARB.SIST_ENDRET_AV_VEILEDERIDENT as ARB_SIST_ENDRET_AV_VEILEDERIDENT,
                               ARB.ENDRINGSTIDSPUNKT            as ARB_ENDRINGSTIDSPUNKT,
                               ARB.OVERSKRIFT                   as ARB_OVERSKRIFT,
                               ARB.FRIST                        as ARB_FRIST,
                               ARB.KATEGORI                     as ARB_KATEGORI
                        FROM OPPFOLGING_DATA OD
                                inner join aktive_identer ai on OD.aktoerid = ai.aktorid
                                 left join oppfolgingsbruker_arena_v2 ob on ob.fodselsnr = ai.fnr
                                 left join nom_skjerming ns on ns.fodselsnr = ai.fnr
                                 left join bruker_data bd on bd.freg_ident = ai.fnr
                                 LEFT JOIN DIALOG D ON D.AKTOERID = ai.aktorid
                                 LEFT JOIN VEDTAKSTATUS V on V.AKTOERID = ai.aktorid
                                 LEFT JOIN ARBEIDSLISTE ARB on ARB.AKTOERID = ai.aktorid
                                 LEFT JOIN BRUKER_PROFILERING BP ON BP.AKTOERID = ai.aktorid
                                 LEFT JOIN BRUKER_CV CV on CV.AKTOERID = ai.aktorid
                                 LEFT JOIN BRUKER_REGISTRERING BR on BR.AKTOERID = ai.aktorid
                                 LEFT JOIN YTELSE_STATUS_FOR_BRUKER YB on YB.AKTOERID = ai.aktorid
                                 where ai.aktorid = ANY (?::varchar[])
                        """,
                (ResultSet rs) -> {
                    while (rs.next()) {
                        result.add(mapTilOppfolgingsBruker(rs, logdiff));
                    }
                    return result;
                }, params);
    }

    @SneakyThrows
    private OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs, boolean logDiff) {
        if (logDiff) {
            logDiff(rs);
        }
        String formidlingsgruppekode = rs.getString(FORMIDLINGSGRUPPEKODE);
        String kvalifiseringsgruppekode = rs.getString(KVALIFISERINGSGRUPPEKODE);

        String fnr = rs.getString(FODSELSNR);
        String vedtakstatus = rs.getString(VEDTAKSTATUS);
        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setFnr(fnr)
                .setAktoer_id(rs.getString(AKTOERID))
                .setBrukers_situasjon(rs.getString(BRUKERS_SITUASJON))
                .setProfilering_resultat(rs.getString(PROFILERING_RESULTAT))
                .setUtdanning(rs.getString(UTDANNING))
                .setUtdanning_bestatt(rs.getString(UTDANNING_BESTATT))
                .setUtdanning_godkjent(rs.getString(UTDANNING_GODKJENT))
                .setHar_delt_cv(rs.getBoolean(HAR_DELT_CV))
                .setCv_eksistere(rs.getBoolean(CV_EKSISTERER))
                .setOppfolging(rs.getBoolean(OPPFOLGING))
                .setNy_for_veileder(rs.getBoolean(NY_FOR_VEILEDER))
                .setVeileder_id(rs.getString(VEILEDERID))
                .setManuell_bruker(rs.getBoolean(MANUELL) ? "MANUELL" : null)
                .setOppfolging_startdato(toIsoUTC(rs.getTimestamp(STARTDATO)))
                .setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp(VENTER_PA_BRUKER)))
                .setVenterpasvarfranav(toIsoUTC(rs.getTimestamp(VENTER_PA_NAV)))
                .setVedtak_status(
                        Optional.ofNullable(vedtakstatus)
                                .map(KafkaVedtakStatusEndring.VedtakStatusEndring::valueOf)
                                .map(KafkaVedtakStatusEndring::vedtakStatusTilTekst)
                                .orElse(null)
                )
                .setVedtak_status_endret(toIsoUTC(rs.getTimestamp(VEDTAKSTATUS_ENDRET_TIDSPUNKT)))
                .setAnsvarlig_veileder_for_vedtak(rs.getString(VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN))
                .setYtelse(rs.getString(YTELSE))
                .setUtlopsdato(toIsoUTC(rs.getTimestamp(YTELSE_UTLOPSDATO)))
                .setDagputlopuke(rs.getObject(DAGPUTLOPUKE, Integer.class))
                .setPermutlopuke(rs.getObject(PERMUTLOPUKE, Integer.class))
                .setAapmaxtiduke(rs.getObject(AAPMAXTIDUKE, Integer.class))
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getObject(AAPUNNTAKDAGERIGJEN, Integer.class)));

        if (brukNOMSkjerming(unleashService)) {
            bruker.setEgen_ansatt(rs.getBoolean(ER_SKJERMET));
        } else {
            bruker.setEgen_ansatt(rs.getBoolean(SPERRET_ANSATT_ARENA));
        }
        String arbeidslisteTidspunkt = toIsoUTC(rs.getTimestamp(ARB_ENDRINGSTIDSPUNKT));
        if (arbeidslisteTidspunkt != null) {
            bruker.setArbeidsliste_aktiv(true)
                    .setArbeidsliste_endringstidspunkt(arbeidslisteTidspunkt)
                    .setArbeidsliste_frist(Optional.ofNullable(toIsoUTC(rs.getTimestamp(ARB_FRIST))).orElse(getFarInTheFutureDate()))
                    .setArbeidsliste_kategori(rs.getString(ARB_KATEGORI))
                    .setArbeidsliste_sist_endret_av_veilederid(rs.getString(ARB_SIST_ENDRET_AV_VEILEDERIDENT));
            String overskrift = rs.getString(ARB_OVERSKRIFT);

            bruker.setArbeidsliste_tittel_lengde(
                    Optional.ofNullable(overskrift)
                            .map(String::length)
                            .orElse(0));
            bruker.setArbeidsliste_tittel_sortering(
                    Optional.ofNullable(overskrift)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s.substring(0, Math.min(2, s.length())))
                            .orElse(""));
        } else {
            bruker.setArbeidsliste_aktiv(false);
        }

        Date foedsels_dato = rs.getDate("foedselsdato");
        if (!brukPDLBrukerdata(unleashService)) {
            flettInnDataFraArena(rs, bruker);
        } else if (brukPDLBrukerdata(unleashService) && foedsels_dato != null) {
            flettInnDataFraPDL(rs, bruker);
        } else if (brukArenaSomBackup(unleashService)) {
            log.info("Fant ikke brukerdat på aktor: {}, bruker arena som backup", bruker.getAktoer_id());
            flettInnDataFraArena(rs, bruker);
        } else {
            log.error("Fant ikke brukerdat på aktor: {}, antar derfor at bruker er skjermet", bruker.getAktoer_id());
            bruker.setFnr("");
        }

        // ARENA DB LENKE: skal fjernes på sikt
        return bruker
                .setEnhet_id(rs.getString(NAV_KONTOR))
                .setIserv_fra_dato(toIsoUTC(rs.getTimestamp(ISERV_FRA_DATO)))
                .setRettighetsgruppekode(rs.getString(RETTIGHETSGRUPPEKODE))
                .setHovedmaalkode(rs.getString(HOVEDMAALKODE))
                .setSikkerhetstiltak(rs.getString(SIKKERHETSTILTAK_TYPE_KODE))
                .setDiskresjonskode(rs.getString(DISKRESJONSKODE))
                .setFormidlingsgruppekode(formidlingsgruppekode)
                .setKvalifiseringsgruppekode(kvalifiseringsgruppekode)
                .setTrenger_vurdering(OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setEr_sykmeldt_med_arbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, vedtakstatus));
    }

    @SneakyThrows
    private void flettInnDataFraPDL(ResultSet rs, OppfolgingsBruker bruker) {
        Date foedsels_dato = rs.getDate("foedselsdato");
        String fornavn = rs.getString("fornavn_pdl");
        String etternavn = rs.getString("etternavn_pdl");
        bruker
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", etternavn, fornavn))
                .setEr_doed(rs.getBoolean("er_doed_pdl"))
                .setFodselsdag_i_mnd(foedsels_dato.toLocalDate().getDayOfMonth())
                .setFodselsdato(lagFodselsdato(foedsels_dato.toLocalDate()))
                .setKjonn(rs.getString("kjoenn"));
    }

    @SneakyThrows
    private void flettInnDataFraArena(ResultSet rs, OppfolgingsBruker bruker) {
        String fnr = rs.getString(FODSELSNR);
        String fornavn = rs.getString(FORNAVN);
        String etternavn = rs.getString(ETTERNAVN);
        bruker
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", etternavn, fornavn))
                .setEr_doed(rs.getBoolean(ER_DOED))
                .setFodselsdag_i_mnd(Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(fnr)))
                .setFodselsdato(lagFodselsdato(fnr))
                .setKjonn(FodselsnummerUtils.lagKjonn(fnr));
    }

    @SneakyThrows
    private void logDiff(ResultSet rs) {
        Date foedsels_dato = rs.getDate("foedselsdato");
        String aktoerId = rs.getString(AKTOERID);
        String fnr = rs.getString(FODSELSNR);
        if (foedsels_dato == null) {
            log.info("Arena/PDL: Har ikke PDL data på aktoer: {}", aktoerId);
            return;
        }
        if (isDifferent(rs.getString("fornavn_pdl"), rs.getString(FORNAVN))) {
            log.info("Arena/PDL: fornavn feil bruker: {}", aktoerId);
        }
        if (isDifferent(rs.getString("etternavn_pdl"), rs.getString(ETTERNAVN))) {
            log.info("Arena/PDL: etternavn feil bruker: {}", aktoerId);
        }
        if (isDifferent(rs.getBoolean("er_doed_pdl"), rs.getBoolean(ER_DOED))) {
            log.info("Arena/PDL: er_doed_pdl feil bruker: {}, pdl: {}, arena: {}", aktoerId, rs.getBoolean("er_doed_pdl"), rs.getBoolean(ER_DOED));
        }
        if (isDifferent(rs.getString("kjoenn"), FodselsnummerUtils.lagKjonn(fnr))) {
            log.info("Arena/PDL: kjønn feil bruker: {}", aktoerId);
        }
        if (isDifferent(lagFodselsdato(foedsels_dato.toLocalDate()), lagFodselsdato(fnr))) {
            log.info("Arena/PDL: Fodselsdato feil bruker: {}", aktoerId);
        }
        if (isDifferent(foedsels_dato.toLocalDate().getDayOfMonth(), Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(fnr)))) {
            log.info("Arena/PDL: Fodselsdag_i_mnd feil bruker: {}", aktoerId);
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
}
