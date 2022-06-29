package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.Landgruppe;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.Kafka14aStatusendring;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseUtils.konverterDagerTilUker;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.*;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
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

    private final KodeverkService kodeverskService;

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds) {
        return hentOppfolgingsBrukere(aktorIds, false);
    }

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds, boolean logdiff) {
        List<OppfolgingsBruker> result = new ArrayList<>();

        var params = aktorIds.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        return db.query("""
                        select OD.AKTOERID, OD.OPPFOLGING, ob.*,
                               ns.er_skjermet, ai.fnr, bd.foedselsdato, bd.fornavn as fornavn_pdl,
                               bd.etternavn as etternavn_pdl, bd.mellomnavn as mellomnavn_pdl, bd.er_doed as er_doed_pdl, bd.kjoenn,
                               bd.foedeland, bd.innflyttingTilNorgeFraLand, bd.angittFlyttedato,
                               bd.talespraaktolk, bd.tegnspraaktolk, bd.tolkbehovsistoppdatert,
                               OD.STARTDATO, OD.NY_FOR_VEILEDER, OD.VEILEDERID, OD.MANUELL,  DI.VENTER_PA_BRUKER,  DI.VENTER_PA_NAV,
                               U.VEDTAKSTATUS, BP.PROFILERING_RESULTAT, CV.HAR_DELT_CV, CV.CV_EKSISTERER, BR.BRUKERS_SITUASJON,
                               BR.UTDANNING, BR.UTDANNING_BESTATT, BR.UTDANNING_GODKJENT, YB.YTELSE, YB.AAPMAXTIDUKE, YB.AAPUNNTAKDAGERIGJEN,
                               YB.DAGPUTLOPUKE, YB.PERMUTLOPUKE, YB.UTLOPSDATO as YTELSE_UTLOPSDATO,
                               U.ANSVARLIG_VEILDERNAVN          as VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN,
                               U.ENDRET_TIDSPUNKT               as VEDTAKSTATUS_ENDRET_TIDSPUNKT,
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
                                 LEFT JOIN DIALOG DI ON DI.AKTOERID = ai.aktorid
                                 LEFT JOIN UTKAST_14A_STATUS U on U.AKTOERID = ai.aktorid
                                 LEFT JOIN ARBEIDSLISTE ARB on ARB.AKTOERID = ai.aktorid
                                 LEFT JOIN BRUKER_PROFILERING BP ON BP.AKTOERID = ai.aktorid
                                 LEFT JOIN BRUKER_CV CV on CV.AKTOERID = ai.aktorid
                                 LEFT JOIN BRUKER_REGISTRERING BR on BR.AKTOERID = ai.aktorid
                                 LEFT JOIN YTELSE_STATUS_FOR_BRUKER YB on YB.AKTOERID = ai.aktorid
                                 where ai.aktorid = ANY (?::varchar[])
                        """,
                (ResultSet rs) -> {
                    while (rs.next()) {
                        OppfolgingsBruker bruker = mapTilOppfolgingsBruker(rs, logdiff);
                        if (bruker.getFnr() == null) {
                            continue; // NB: Dolly brukere kan ha kun aktoerId, dette vil også gjelde personer med kun NPID
                        }
                        if (rs.getString(FODSELSNR_ARENA) == null) {
                            leggTilHistoriskArenaDataHvisTilgjengelig(bruker);
                        }
                        result.add(bruker);
                    }
                    return result;
                }, params);
    }

    private void leggTilHistoriskArenaDataHvisTilgjengelig(OppfolgingsBruker bruker) {
        long startTime = System.currentTimeMillis();
        OppfolgingsBruker brukerMedHistoriskData = queryForObjectOrNull(() ->
                db.queryForObject("""
                        select * from oppfolgingsbruker_arena_v2 ob
                        where ob.fodselsnr in
                            (select ident from bruker_identer where person =
                                (select person from bruker_identer where ident = ?)
                            )
                        order by ob.endret_dato desc
                        limit 1
                        """, (rs, i) -> flettInnOppfolgingsbruker(bruker, bruker.getUtkast_14a_status(), rs), bruker.getFnr())
        );
        long endTime = System.currentTimeMillis();
        log.info("Ytelse, søkte opp historisk arena data på: {}ms", endTime - startTime);
        if (brukerMedHistoriskData != null && brukerMedHistoriskData.getEnhet_id() != null) {
            log.info("Bruker historisk ident i arena for aktor: {}", bruker.getAktoer_id());
        }
    }

    @SneakyThrows
    private OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs, boolean logDiff) {
        if (logDiff) {
            logDiff(rs);
        }

        String fnr = rs.getString(FODSELSNR);
        String utkast14aStatus = rs.getString(UTKAST_14A_STATUS);
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
                .setUtkast_14a_status(Optional.ofNullable(utkast14aStatus)
                        .map(Kafka14aStatusendring.Status::valueOf)
                        .map(Kafka14aStatusendring::statusTilTekst)
                        .orElse(null))
                .setUtkast_14a_status_endret(toIsoUTC(rs.getTimestamp(UTKAST_14A_ENDRET_TIDSPUNKT)))
                .setUtkast_14a_ansvarlig_veileder(rs.getString(UTKAST_14A_ANSVARLIG_VEILDERNAVN))
                .setYtelse(rs.getString(YTELSE))
                .setUtlopsdato(toIsoUTC(rs.getTimestamp(YTELSE_UTLOPSDATO)))
                .setDagputlopuke(rs.getObject(DAGPUTLOPUKE, Integer.class))
                .setPermutlopuke(rs.getObject(PERMUTLOPUKE, Integer.class))
                .setAapmaxtiduke(rs.getObject(AAPMAXTIDUKE, Integer.class))
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getObject(AAPUNNTAKDAGERIGJEN, Integer.class)));

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

        // ARENA DB LENKE: skal fjernes på sikt
        flettInnOppfolgingsbruker(bruker, utkast14aStatus, rs);

        Date foedsels_dato = rs.getDate("foedselsdato");
        if (brukPDLBrukerdata(unleashService) && foedsels_dato != null) {
            flettInnDataFraPDL(rs, bruker);
        } else if (brukArenaSomBackup(unleashService)) {
            flettInnPersonDataFraArena(rs, bruker);
        } else if (isDevelopment().orElse(false)) {
            bruker.setFnr(null); // Midlertidig forsikring for at brukere i q1 aldri har ekte data. Fjernes sammen med toggles, og bruk av inner join for brukerdata
        }
        return bruker;
    }

    @SneakyThrows
    private OppfolgingsBruker flettInnOppfolgingsbruker(OppfolgingsBruker bruker, String utkast14aStatus, ResultSet rs) {
        String fnr = rs.getString(FODSELSNR_ARENA);
        if (fnr == null) {
            return bruker;
        }
        if (!brukPDLBrukerdata(unleashService) && isProduction().orElse(false)) {
            flettInnPersonDataFraArena(rs, bruker);
        }
        if (!brukNOMSkjerming(unleashService)) {
            bruker.setEgen_ansatt(rs.getBoolean(SPERRET_ANSATT_ARENA));
        }
        String formidlingsgruppekode = rs.getString(FORMIDLINGSGRUPPEKODE);
        String kvalifiseringsgruppekode = rs.getString(KVALIFISERINGSGRUPPEKODE);
        return bruker
                .setFnr(fnr)
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
                .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, utkast14aStatus));
    }

    @SneakyThrows
    private void flettInnPersonDataFraArena(ResultSet rs, OppfolgingsBruker bruker) {
        String fnr = rs.getString(FODSELSNR_ARENA);
        if (fnr == null) {
            return;
        }
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
    private void flettInnDataFraPDL(ResultSet rs, OppfolgingsBruker bruker) {
        Date foedsels_dato = rs.getDate("foedselsdato");
        String mellomnavn = rs.getString("mellomnavn_pdl");
        String fornavn = rs.getString("fornavn_pdl");
        if (mellomnavn != null) {
            fornavn += " " + mellomnavn;
        }
        String etternavn = rs.getString("etternavn_pdl");

        String landGruppe = Landgruppe.getInstance().getLandgruppeForLandKode(rs.getString("foedeland"));
        String foedelandFulltNavn = kodeverskService.getBeskrivelseForLandkode(rs.getString("foedeland"));
        String innflyttingTilNorgeFraLandFullNavn = kodeverskService.getBeskrivelseForLandkode(rs.getString("innflyttingTilNorgeFraLand"));
        String taleSpraakFulltNavn = kodeverskService.getBeskrivelseForSpraakKode(rs.getString("talespraaktolk"));
        String tegnSpraakFulltNavn = kodeverskService.getBeskrivelseForSpraakKode(rs.getString("tegnspraaktolk"));
        bruker
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", etternavn, fornavn))
                .setEr_doed(rs.getBoolean("er_doed_pdl"))
                .setFodselsdag_i_mnd(foedsels_dato.toLocalDate().getDayOfMonth())
                .setFodselsdato(lagFodselsdato(foedsels_dato.toLocalDate()))
                .setFoedeland(rs.getString("foedeland"))
                .setFoedelandFulltNavn(foedelandFulltNavn)
                .setKjonn(rs.getString("kjoenn"))
                .setTalespraaktolk(taleSpraakFulltNavn)
                .setTegnspraaktolk(tegnSpraakFulltNavn)
                .setTolkBehovSistOppdatert(DateUtils.toLocalDateOrNull(rs.getString("tolkBehovSistOppdatert")))
                .setInnflyttingTilNorgeFraLand(innflyttingTilNorgeFraLandFullNavn)
                .setLandgruppe(landGruppe);
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
        if (isDifferent(rs.getString("fornavn_pdl").toLowerCase(), rs.getString(FORNAVN).toLowerCase())) {
            log.info("Arena/PDL: fornavn feil bruker: {}", aktoerId);
        }
        if (isDifferent(rs.getString("etternavn_pdl").toLowerCase(), rs.getString(ETTERNAVN).toLowerCase())) {
            log.info("Arena/PDL: etternavn feil bruker: {}", aktoerId);
        }
        if (isDifferent(rs.getBoolean("er_doed_pdl"), rs.getBoolean(ER_DOED))) {
            log.info("Arena/PDL: er_doed_pdl feil bruker: {}, pdl: {}, arena: {}", aktoerId, rs.getBoolean("er_doed_pdl"), rs.getBoolean(ER_DOED));
        }
        if (isDifferent(rs.getString("kjoenn").toLowerCase(), FodselsnummerUtils.lagKjonn(fnr).toLowerCase())) {
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
