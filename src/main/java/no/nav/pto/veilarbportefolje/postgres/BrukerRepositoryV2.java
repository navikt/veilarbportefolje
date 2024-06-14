package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteMapper;
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.Landgruppe;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.Kafka14aStatusendring;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseUtils.konverterDagerTilUker;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OpensearchData.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static no.nav.pto.veilarbportefolje.util.FodselsnummerUtils.lagFodselsdato;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BrukerRepositoryV2 {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate db;

    private final KodeverkService kodeverskService;

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds) {
        return hentOppfolgingsBrukere(aktorIds, false);
    }

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds, boolean logdiff) {
        List<OppfolgingsBruker> result = new ArrayList<>();

        var params = aktorIds.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        return db.query("""
                        select OD.AKTOERID, OD.OPPFOLGING, ob.*,
                               ns.er_skjermet, ns.skjermet_til, ai.fnr, bd.foedselsdato, bd.fornavn as fornavn_pdl,
                               bd.etternavn as etternavn_pdl, bd.mellomnavn as mellomnavn_pdl, bd.er_doed as er_doed_pdl, bd.kjoenn,
                               bd.foedeland,
                               bd.talespraaktolk, bd.tegnspraaktolk, bd.tolkbehovsistoppdatert, bd.diskresjonkode,
                               bd.sikkerhetstiltak_type, bd.sikkerhetstiltak_gyldigfra, bd.sikkerhetstiltak_gyldigtil, bd.sikkerhetstiltak_beskrivelse,
                               bd.bydelsnummer, bd.kommunenummer, bd.bostedsistoppdatert, bd.utenlandskadresse, bd.harUkjentBosted,
                               OD.STARTDATO, OD.NY_FOR_VEILEDER, OD.VEILEDERID, OD.MANUELL,  DI.VENTER_PA_BRUKER,  DI.VENTER_PA_NAV,
                               U.VEDTAKSTATUS, BP.PROFILERING_RESULTAT, CV.HAR_DELT_CV, CV.CV_EKSISTERER, BR.BRUKERS_SITUASJON, BR.REGISTRERING_OPPRETTET,
                               BR.UTDANNING, BR.UTDANNING_BESTATT, BR.UTDANNING_GODKJENT, EiR.BRUKERS_SITUASJON as ENDRET_BRUKERS_SITUASJON, EiR.BRUKERS_SITUASJON_SIST_ENDRET, YB.YTELSE, YB.AAPMAXTIDUKE, YB.AAPUNNTAKDAGERIGJEN,
                               YB.DAGPUTLOPUKE, YB.PERMUTLOPUKE, YB.UTLOPSDATO as YTELSE_UTLOPSDATO, YB.ANTALLDAGERIGJEN, YB.ENDRET_DATO as YTELSE_ENDRET_DATO,
                               U.ANSVARLIG_VEILDERNAVN          as VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN,
                               U.ENDRET_TIDSPUNKT               as VEDTAKSTATUS_ENDRET_TIDSPUNKT,
                               ARB.SIST_ENDRET_AV_VEILEDERIDENT as ARB_SIST_ENDRET_AV_VEILEDERIDENT,
                               ARB.ENDRINGSTIDSPUNKT            as ARB_ENDRINGSTIDSPUNKT,
                               ARB.OVERSKRIFT                   as ARB_OVERSKRIFT,
                               ARB.FRIST                        as ARB_FRIST,
                               ARB.KATEGORI                     as ARB_KATEGORI,
                               ARB.NAV_KONTOR_FOR_ARBEIDSLISTE  as ARB_NAV_KONTOR_FOR_ARBEIDSLISTE,
                               FAR.verdi                        as FAR_VERDI,
                               FAR.enhet_id                     as FAR_ENHET_ID,
                               HL.frist							as HL_FRIST,
                               HL.kommentar						as HL_KOMMENTAR,
                               HL.endret_dato                   as HL_ENDRET_DATO,
                               hl.endret_av_veileder            as HL_ENDRET_AV,
                               HL.huskelapp_id                  as HL_HUSKELAPPID,
                               HL.enhet_id                      as HL_ENHET_ID
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
                                 LEFT JOIN ENDRING_I_REGISTRERING EiR on EiR.AKTOERID = ai.aktorid
                                 LEFT JOIN fargekategori far on far.fnr = ai.fnr
                                 LEFT JOIN HUSKELAPP HL on HL.fnr = ai.fnr and HL.status = 'AKTIV'
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
            secureLog.info("Bruker historisk ident i arena for aktor: {}", bruker.getAktoer_id());
        }
    }

    @SneakyThrows
    private OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs, boolean logDiff) {
        if (logDiff) {
            logDiff(rs);
        }

        String fnr = rs.getString(FODSELSNR);
        String utkast14aStatus = rs.getString(UTKAST_14A_STATUS);

        LocalDate aapordinerutlopsdato = null;
        boolean erSpesieltTilpassetInnsats = (rs.getString(KVALIFISERINGSGRUPPEKODE) != null && rs.getString(KVALIFISERINGSGRUPPEKODE).equals("BATT"));
        if (erSpesieltTilpassetInnsats) {
            aapordinerutlopsdato = DateUtils.addWeeksToTodayAndGetNthDay(rs.getTimestamp("YTELSE_ENDRET_DATO"), rs.getInt(AAPMAXTIDUKE), rs.getInt(ANTALLDAGERIGJEN));
        }


        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setFnr(fnr)
                .setAktoer_id(rs.getString(AKTOERID))
                .setProfilering_resultat(rs.getString(PROFILERING_RESULTAT))
                .setUtdanning(rs.getString(UTDANNING))
                .setUtdanning_bestatt(rs.getString(UTDANNING_BESTATT))
                .setUtdanning_godkjent(rs.getString(UTDANNING_GODKJENT))
                .setUtdanning_og_situasjon_sist_endret(toLocalDate(rs.getTimestamp(REGISTRERING_OPPRETTET)))
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
                .setAapordinerutlopsdato(aapordinerutlopsdato)
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getObject(AAPUNNTAKDAGERIGJEN, Integer.class)))
                .setFargekategori(rs.getString(FAR_VERDI))
                .setFargekategori_enhetId(rs.getString(FAR_ENHET_ID));

        setHuskelapp(bruker, rs);
        setBrukersSituasjon(bruker, rs);

        String arbeidslisteTidspunkt = toIsoUTC(rs.getTimestamp(ARB_ENDRINGSTIDSPUNKT));
        if (arbeidslisteTidspunkt != null) {
            String fargekategoriFraFargekategoriTabell = rs.getString(FAR_VERDI);
            String fargekategoriFraArbeidslisteTabell = rs.getString(ARB_KATEGORI);
            String resolvedFargekategori = fargekategoriFraFargekategoriTabell != null
                    ? ArbeidslisteMapper.mapFraFargekategoriTilKategori(fargekategoriFraFargekategoriTabell).name()
                    : fargekategoriFraArbeidslisteTabell;

            bruker.setArbeidsliste_aktiv(true)
                    .setArbeidsliste_endringstidspunkt(arbeidslisteTidspunkt)
                    .setArbeidsliste_frist(Optional.ofNullable(toIsoUTC(rs.getTimestamp(ARB_FRIST))).orElse(getFarInTheFutureDate()))
                    .setArbeidsliste_kategori(resolvedFargekategori)
                    .setArbeidsliste_sist_endret_av_veilederid(rs.getString(ARB_SIST_ENDRET_AV_VEILEDERIDENT))
                    .setNavkontor_for_arbeidsliste(rs.getString(ARB_NAV_KONTOR_FOR_ARBEIDSLISTE));
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
        if (foedsels_dato != null) {
            flettInnDataFraPDL(rs, bruker);
        } else if (isDevelopment().orElse(false)) {
            bruker.setFnr(null); // Midlertidig forsikring for at brukere i q1 aldri har ekte data. Fjernes sammen med toggles, og bruk av inner join for brukerdata
        }
        bruker.setEgen_ansatt(rs.getBoolean(ER_SKJERMET));
        bruker.setSkjermet_til(toLocalDateTimeOrNull(rs.getTimestamp(SKJERMET_TIL)));

        return bruker;
    }

    @SneakyThrows
    private void setHuskelapp(OppfolgingsBruker oppfolgingsBruker, ResultSet rs) {
        LocalDate frist = toLocalDate(rs.getTimestamp(HL_FRIST));
        String kommentar = rs.getString(HL_KOMMENTAR);
        String huskelappId = rs.getString(HL_HUSKELAPPID);
        LocalDate endretDato = toLocalDate(rs.getTimestamp(HL_ENDRET_DATO));
        VeilederId endretAv = VeilederId.veilederIdOrNull(rs.getString(HL_ENDRET_AV));
        String enhetId = rs.getString(HL_ENHET_ID);
        if (frist != null || kommentar != null) {
            oppfolgingsBruker.setHuskelapp(new HuskelappForBruker(frist, kommentar, endretDato, endretAv.getValue(), huskelappId, enhetId));
        }
    }

    @SneakyThrows
    private void setBrukersSituasjon(OppfolgingsBruker oppfolgingsBruker, ResultSet rs) {
        boolean harOppdatertBrukersSituasjon = rs.getString(ENDRET_BRUKERS_SITUASJON) != null && rs.getTimestamp(BRUKERS_SITUASJON_SIST_ENDRET) != null;
        LocalDate oppdatertBrukesSituasjonSistEndretDato = toLocalDate(rs.getTimestamp(BRUKERS_SITUASJON_SIST_ENDRET));
        LocalDate brukesSituasjonOpprettetDato = toLocalDate(rs.getTimestamp(REGISTRERING_OPPRETTET));
        boolean harEndretSituasjonEttterRegistrering = harOppdatertBrukersSituasjon && isEqualOrAfterWithNullCheck(oppdatertBrukesSituasjonSistEndretDato, brukesSituasjonOpprettetDato);
        String brukersSisteSituasjon = harEndretSituasjonEttterRegistrering ? rs.getString(ENDRET_BRUKERS_SITUASJON) : rs.getString(BRUKERS_SITUASJON);
        LocalDate brukersSituasjonSistEndretDato = harEndretSituasjonEttterRegistrering ? oppdatertBrukesSituasjonSistEndretDato : brukesSituasjonOpprettetDato;

        oppfolgingsBruker.setBrukers_situasjoner(brukersSisteSituasjon == null ? emptyList() : List.of(brukersSisteSituasjon));
        oppfolgingsBruker.setBrukers_situasjon_sist_endret(brukersSituasjonSistEndretDato);
    }

    @SneakyThrows
    private OppfolgingsBruker flettInnOppfolgingsbruker(OppfolgingsBruker bruker, String utkast14aStatus, ResultSet rs) {
        String fnr = rs.getString(FODSELSNR_ARENA);
        if (fnr == null) {
            return bruker;
        }

        String formidlingsgruppekode = rs.getString(FORMIDLINGSGRUPPEKODE);
        String kvalifiseringsgruppekode = rs.getString(KVALIFISERINGSGRUPPEKODE);
        return bruker
                .setFnr(fnr)
                .setEnhet_id(rs.getString(NAV_KONTOR))
                .setIserv_fra_dato(toIsoUTC(rs.getTimestamp(ISERV_FRA_DATO)))
                .setRettighetsgruppekode(rs.getString(RETTIGHETSGRUPPEKODE))
                .setHovedmaalkode(rs.getString(HOVEDMAALKODE))
                .setFormidlingsgruppekode(formidlingsgruppekode)
                .setKvalifiseringsgruppekode(kvalifiseringsgruppekode)
                .setTrenger_vurdering(OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setEr_sykmeldt_med_arbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, utkast14aStatus));
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

        Date sikkerhetstiltakGyldigtil = rs.getDate("sikkerhetstiltak_gyldigtil");
        boolean showSikkerhetsTiltak = (sikkerhetstiltakGyldigtil == null || sikkerhetstiltakGyldigtil.toLocalDate().isAfter(LocalDate.now()));

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
                .setTalespraaktolk(rs.getString("talespraaktolk"))
                .setTegnspraaktolk(rs.getString("tegnspraaktolk"))
                .setTolkBehovSistOppdatert(DateUtils.toLocalDateOrNull(rs.getString("tolkBehovSistOppdatert")))
                .setLandgruppe((landGruppe != null && !landGruppe.isEmpty()) ? landGruppe : null)
                .setBydelsnummer(rs.getString("bydelsnummer"))
                .setKommunenummer(rs.getString("kommunenummer"))
                .setUtenlandskAdresse(rs.getString("utenlandskAdresse"))
                .setHarUkjentBosted(rs.getBoolean("harUkjentBosted"))
                .setBostedSistOppdatert(toLocalDateOrNull(rs.getString("bostedSistOppdatert")))
                .setSikkerhetstiltak(showSikkerhetsTiltak ? rs.getString("sikkerhetstiltak_type") : null)
                .setSikkerhetstiltak_gyldig_fra(showSikkerhetsTiltak ? rs.getString("sikkerhetstiltak_gyldigfra") : null)
                .setSikkerhetstiltak_gyldig_til(showSikkerhetsTiltak ? rs.getString("sikkerhetstiltak_gyldigtil") : null)
                .setSikkerhetstiltak_beskrivelse(showSikkerhetsTiltak ? rs.getString("sikkerhetstiltak_beskrivelse") : null)
                .setDiskresjonskode(rs.getString("diskresjonkode"));
    }

    @SneakyThrows
    private void logDiff(ResultSet rs) {
        Date foedsels_dato = rs.getDate("foedselsdato");
        String aktoerId = rs.getString(AKTOERID);
        String fnr = rs.getString(FODSELSNR);
        if (foedsels_dato == null) {
            secureLog.info("Arena/PDL: Har ikke PDL data på aktoer: {}", aktoerId);
            return;
        }
        if (isDifferent(rs.getString("fornavn_pdl").toLowerCase(), rs.getString(FORNAVN).toLowerCase())) {
            secureLog.info("Arena/PDL: fornavn feil bruker: {}", aktoerId);
        }
        if (isDifferent(rs.getString("etternavn_pdl").toLowerCase(), rs.getString(ETTERNAVN).toLowerCase())) {
            secureLog.info("Arena/PDL: etternavn feil bruker: {}", aktoerId);
        }
        if (isDifferent(rs.getBoolean("er_doed_pdl"), rs.getBoolean(ER_DOED))) {
            secureLog.info("Arena/PDL: er_doed_pdl feil bruker: {}, pdl: {}, arena: {}", aktoerId, rs.getBoolean("er_doed_pdl"), rs.getBoolean(ER_DOED));
        }
        if (isDifferent(rs.getString("kjoenn").toLowerCase(), FodselsnummerUtils.lagKjonn(fnr).toLowerCase())) {
            secureLog.info("Arena/PDL: kjønn feil bruker: {}", aktoerId);
        }
        if (isDifferent(lagFodselsdato(foedsels_dato.toLocalDate()), lagFodselsdato(fnr))) {
            secureLog.info("Arena/PDL: Fodselsdato feil bruker: {}", aktoerId);
        }
        if (isDifferent(foedsels_dato.toLocalDate().getDayOfMonth(), Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(fnr)))) {
            secureLog.info("Arena/PDL: Fodselsdag_i_mnd feil bruker: {}", aktoerId);
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
