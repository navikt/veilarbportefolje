package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteMapper;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.Landgruppe;
import no.nav.pto.veilarbportefolje.util.DateUtils;
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
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPENSEARCHDATA.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BrukerRepositoryV2 {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate db;

    private final KodeverkService kodeverkService;

    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds) {
        List<OppfolgingsBruker> result = new ArrayList<>();

        var params = aktorIds.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        return db.query("""
                        SELECT
                               OPPFOLGING_DATA.AKTOERID                                 as OPPFOLGING_DATA_AKTOERID,
                               OPPFOLGING_DATA.STARTDATO                                as OPPFOLGING_DATA_STARTDATO,
                               OPPFOLGING_DATA.NY_FOR_VEILEDER                          as OPPFOLGING_DATA_NY_FOR_VEILEDER,
                               OPPFOLGING_DATA.VEILEDERID                               as OPPFOLGING_DATA_VEILEDERID,
                               OPPFOLGING_DATA.MANUELL                                  as OPPFOLGING_DATA_MANUELL,
                               OPPFOLGING_DATA.OPPFOLGING                               as OPPFOLGING_DATA_OPPFOLGING,
                               AKTIVE_IDENTER.FNR                                       as AKTIVE_IDENTER_FNR,
                               OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR                     as OPPFOLGINGSBRUKER_ARENA_V2_FODSELSNR,
                               OPPFOLGINGSBRUKER_ARENA_V2.FORMIDLINGSGRUPPEKODE         as OPPFOLGINGSBRUKER_ARENA_V2_FORMIDLINGSGRUPPEKODE,
                               OPPFOLGINGSBRUKER_ARENA_V2.ISERV_FRA_DATO                as OPPFOLGINGSBRUKER_ARENA_V2_ISERV_FRA_DATO,
                               OPPFOLGINGSBRUKER_ARENA_V2.NAV_KONTOR                    as OPPFOLGINGSBRUKER_ARENA_V2_NAV_KONTOR,
                               OPPFOLGINGSBRUKER_ARENA_V2.KVALIFISERINGSGRUPPEKODE      as OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE,
                               OPPFOLGINGSBRUKER_ARENA_V2.RETTIGHETSGRUPPEKODE          as OPPFOLGINGSBRUKER_ARENA_V2_RETTIGHETSGRUPPEKODE,
                               OPPFOLGINGSBRUKER_ARENA_V2.HOVEDMAALKODE                 as OPPFOLGINGSBRUKER_ARENA_V2_HOVEDMAALKODE,
                               OPPFOLGINGSBRUKER_ARENA_V2.ENDRET_DATO                   as OPPFOLGINGSBRUKER_ARENA_V2_ENDRET_DATO,
                               NOM_SKJERMING.ER_SKJERMET                                as NOM_SKJERMING_ER_SKJERMET,
                               NOM_SKJERMING.SKJERMET_TIL                               as NOM_SKJERMING_SKJERMET_TIL,
                               BRUKER_DATA.FOEDSELSDATO                                 as BRUKER_DATA_FOEDSELSDATO,
                               BRUKER_DATA.FORNAVN                                      as BRUKER_DATA_FORNAVN,
                               BRUKER_DATA.ETTERNAVN                                    as BRUKER_DATA_ETTERNAVN,
                               BRUKER_DATA.MELLOMNAVN                                   as BRUKER_DATA_MELLOMNAVN,
                               BRUKER_DATA.ER_DOED                                      as BRUKER_DATA_ER_DOED,
                               BRUKER_DATA.KJOENN                                       as BRUKER_DATA_KJOENN,
                               BRUKER_DATA.FOEDELAND                                    as BRUKER_DATA_FOEDELAND,
                               BRUKER_DATA.TALESPRAAKTOLK                               as BRUKER_DATA_TALESPRAAKTOLK,
                               BRUKER_DATA.TEGNSPRAAKTOLK                               as BRUKER_DATA_TEGNSPRAAKTOLK,
                               BRUKER_DATA.TOLKBEHOVSISTOPPDATERT                       as BRUKER_DATA_TOLKBEHOVSISTOPPDATERT,
                               BRUKER_DATA.DISKRESJONKODE                               as BRUKER_DATA_DISKRESJONKODE,
                               BRUKER_DATA.SIKKERHETSTILTAK_TYPE                        as BRUKER_DATA_SIKKERHETSTILTAK_TYPE,
                               BRUKER_DATA.SIKKERHETSTILTAK_GYLDIGFRA                   as BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGFRA,
                               BRUKER_DATA.SIKKERHETSTILTAK_GYLDIGTIL                   as BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGTIL,
                               BRUKER_DATA.SIKKERHETSTILTAK_BESKRIVELSE                 as BRUKER_DATA_SIKKERHETSTILTAK_BESKRIVELSE,
                               BRUKER_DATA.BYDELSNUMMER                                 as BRUKER_DATA_BYDELSNUMMER,
                               BRUKER_DATA.KOMMUNENUMMER                                as BRUKER_DATA_KOMMUNENUMMER,
                               BRUKER_DATA.BOSTEDSISTOPPDATERT                          as BRUKER_DATA_BOSTEDSISTOPPDATERT,
                               BRUKER_DATA.UTENLANDSKADRESSE                            as BRUKER_DATA_UTENLANDSKADRESSE,
                               BRUKER_DATA.HARUKJENTBOSTED                              as BRUKER_DATA_HARUKJENTBOSTED,
                               DIALOG.VENTER_PA_BRUKER                                  as DIALOG_VENTER_PA_BRUKER,
                               DIALOG.VENTER_PA_NAV                                     as DIALOG_VENTER_PA_NAV,
                               UTKAST_14A_STATUS.VEDTAKSTATUS                           as UTKAST_14A_STATUS_VEDTAKSTATUS,
                               UTKAST_14A_STATUS.ANSVARLIG_VEILDERNAVN                  as UTKAST_14A_STATUS_ANSVARLIG_VEILDERNAVN,
                               UTKAST_14A_STATUS.ENDRET_TIDSPUNKT                       as UTKAST_14A_STATUS_ENDRET_TIDSPUNKT,
                               ARBEIDSLISTE.SIST_ENDRET_AV_VEILEDERIDENT                as ARBEIDSLISTE_SIST_ENDRET_AV_VEILEDERIDENT,
                               ARBEIDSLISTE.ENDRINGSTIDSPUNKT                           as ARBEIDSLISTE_ENDRINGSTIDSPUNKT,
                               ARBEIDSLISTE.OVERSKRIFT                                  as ARBEIDSLISTE_OVERSKRIFT,
                               ARBEIDSLISTE.FRIST                                       as ARBEIDSLISTE_FRIST,
                               ARBEIDSLISTE.KATEGORI                                    as ARBEIDSLISTE_KATEGORI,
                               ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE                 as ARBEIDSLISTE_NAV_KONTOR_FOR_ARBEIDSLISTE,
                               BRUKER_PROFILERING.PROFILERING_RESULTAT                  as BRUKER_PROFILERING_PROFILERING_RESULTAT,
                               BRUKER_CV.HAR_DELT_CV                                    as BRUKER_CV_HAR_DELT_CV,
                               BRUKER_CV.CV_EKSISTERER                                  as BRUKER_CV_CV_EKSISTERER,
                               BRUKER_REGISTRERING.BRUKERS_SITUASJON                    as BRUKER_REGISTRERING_BRUKERS_SITUASJON,
                               BRUKER_REGISTRERING.REGISTRERING_OPPRETTET               as BRUKER_REGISTRERING_REGISTRERING_OPPRETTET,
                               BRUKER_REGISTRERING.UTDANNING                            as BRUKER_REGISTRERING_UTDANNING,
                               BRUKER_REGISTRERING.UTDANNING_BESTATT                    as BRUKER_REGISTRERING_UTDANNING_BESTATT,
                               BRUKER_REGISTRERING.UTDANNING_GODKJENT                   as BRUKER_REGISTRERING_UTDANNING_GODKJENT,
                               YTELSE_STATUS_FOR_BRUKER.YTELSE                          as YTELSE_STATUS_FOR_BRUKER_YTELSE,
                               YTELSE_STATUS_FOR_BRUKER.AAPMAXTIDUKE                    as YTELSE_STATUS_FOR_BRUKER_AAPMAXTIDUKE,
                               YTELSE_STATUS_FOR_BRUKER.AAPUNNTAKDAGERIGJEN             as YTELSE_STATUS_FOR_BRUKER_AAPUNNTAKDAGERIGJEN,
                               YTELSE_STATUS_FOR_BRUKER.DAGPUTLOPUKE                    as YTELSE_STATUS_FOR_BRUKER_DAGPUTLOPUKE,
                               YTELSE_STATUS_FOR_BRUKER.PERMUTLOPUKE                    as YTELSE_STATUS_FOR_BRUKER_PERMUTLOPUKE,
                               YTELSE_STATUS_FOR_BRUKER.UTLOPSDATO                      as YTELSE_STATUS_FOR_BRUKER_UTLOPSDATO,
                               YTELSE_STATUS_FOR_BRUKER.ANTALLDAGERIGJEN                as YTELSE_STATUS_FOR_BRUKER_ANTALLDAGERIGJEN,
                               YTELSE_STATUS_FOR_BRUKER.ENDRET_DATO                     as YTELSE_STATUS_FOR_BRUKER_ENDRET_DATO,
                               ENDRING_I_REGISTRERING.BRUKERS_SITUASJON                 as ENDRING_I_REGISTRERING_BRUKERS_SITUASJON,
                               ENDRING_I_REGISTRERING.BRUKERS_SITUASJON_SIST_ENDRET     as ENDRING_I_REGISTRERING_BRUKERS_SITUASJON_SIST_ENDRET,
                               FARGEKATEGORI.VERDI                                      as FARGEKATEGORI_VERDI,
                               FARGEKATEGORI.ENHET_ID                                   as FARGEKATEGORI_ENHET_ID,
                               HUSKELAPP.FRIST                                          as HUSKELAPP_FRIST,
                               HUSKELAPP.KOMMENTAR                                      as HUSKELAPP_KOMMENTAR,
                               HUSKELAPP.ENDRET_DATO                                    as HUSKELAPP_ENDRET_DATO,
                               HUSKELAPP.ENDRET_AV_VEILEDER                             as HUSKELAPP_ENDRET_AV_VEILEDER,
                               HUSKELAPP.HUSKELAPP_ID                                   as HUSKELAPP_HUSKELAPP_ID,
                               HUSKELAPP.ENHET_ID                                       as HUSKELAPP_ENHET_ID,
                               YTELSER_AAP.STATUS                                       as YTELSER_AAP_STATUS,
                               YTELSER_AAP.NYESTE_PERIODE_TOM                           as YTELSER_AAP_NYESTE_PERIODE_TOM
                        from OPPFOLGING_DATA
                                 inner join AKTIVE_IDENTER                              on OPPFOLGING_DATA.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join OPPFOLGINGSBRUKER_ARENA_V2                   on OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR = AKTIVE_IDENTER.FNR
                                 left join NOM_SKJERMING                                on NOM_SKJERMING.FODSELSNR = AKTIVE_IDENTER.FNR
                                 left join BRUKER_DATA                                  on BRUKER_DATA.FREG_IDENT = AKTIVE_IDENTER.FNR
                                 left join DIALOG                                       on DIALOG.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join UTKAST_14A_STATUS                            on UTKAST_14A_STATUS.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join ARBEIDSLISTE                                 on ARBEIDSLISTE.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join BRUKER_PROFILERING                           on BRUKER_PROFILERING.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join BRUKER_CV                                    on BRUKER_CV.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join BRUKER_REGISTRERING                          on BRUKER_REGISTRERING.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join YTELSE_STATUS_FOR_BRUKER                     on YTELSE_STATUS_FOR_BRUKER.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join ENDRING_I_REGISTRERING                       on ENDRING_I_REGISTRERING.AKTOERID = AKTIVE_IDENTER.AKTORID
                                 left join FARGEKATEGORI                                on FARGEKATEGORI.FNR = AKTIVE_IDENTER.FNR
                                 left join HUSKELAPP                                    on HUSKELAPP.FNR = AKTIVE_IDENTER.FNR and HUSKELAPP.STATUS = 'AKTIV'
                                 left join YTELSER_AAP                                  on YTELSER_AAP.NORSK_IDENT = AKTIVE_IDENTER.FNR
                                 where AKTIVE_IDENTER.AKTORID = any (?::varchar[])
                        """,
                (ResultSet rs) -> {
                    while (rs.next()) {
                        OppfolgingsBruker bruker = mapTilOppfolgingsBruker(rs);
                        if (bruker.getFnr() == null) {
                            continue; // NB: Dolly brukere kan ha kun aktoerId, dette vil også gjelde personer med kun NPID
                        }
                        if (rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_FODSELSNR) == null) {
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
                        select
                            FODSELSNR as OPPFOLGINGSBRUKER_ARENA_V2_FODSELSNR,
                            FORMIDLINGSGRUPPEKODE as OPPFOLGINGSBRUKER_ARENA_V2_FORMIDLINGSGRUPPEKODE,
                            KVALIFISERINGSGRUPPEKODE as OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE,
                            NAV_KONTOR as OPPFOLGINGSBRUKER_ARENA_V2_NAV_KONTOR,
                            ISERV_FRA_DATO as OPPFOLGINGSBRUKER_ARENA_V2_ISERV_FRA_DATO,
                            RETTIGHETSGRUPPEKODE as OPPFOLGINGSBRUKER_ARENA_V2_RETTIGHETSGRUPPEKODE,
                            HOVEDMAALKODE as OPPFOLGINGSBRUKER_ARENA_V2_HOVEDMAALKODE
                        from OPPFOLGINGSBRUKER_ARENA_V2
                        where FODSELSNR in
                            (select IDENT from BRUKER_IDENTER where PERSON =
                                (select PERSON from BRUKER_IDENTER where IDENT = ?)
                            )
                        order by ENDRET_DATO desc
                        limit 1
                        """, (rs, i) -> flettInnOppfolgingsbruker(bruker, rs), bruker.getFnr())
        );
        long endTime = System.currentTimeMillis();
        log.info("Ytelse, søkte opp historisk arena data på: {}ms", endTime - startTime);
        if (brukerMedHistoriskData != null && brukerMedHistoriskData.getEnhet_id() != null) {
            secureLog.info("Bruker historisk ident i arena for aktor: {}", bruker.getAktoer_id());
        }
    }

    @SneakyThrows
    private OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs) {
        String fnr = rs.getString(AKTIVE_IDENTER_FNR);
        String utkast14aStatus = rs.getString(UTKAST_14A_STATUS_VEDTAKSTATUS);

        LocalDate aapordinerutlopsdato = null;
        boolean erSpesieltTilpassetInnsats = (rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE) != null && rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE).equals("BATT"));
        if (erSpesieltTilpassetInnsats) {
            aapordinerutlopsdato = DateUtils.addWeeksToTodayAndGetNthDay(rs.getTimestamp(YTELSE_STATUS_FOR_BRUKER_ENDRET_DATO), rs.getInt(YTELSE_STATUS_FOR_BRUKER_AAPMAXTIDUKE), rs.getInt(YTELSE_STATUS_FOR_BRUKER_ANTALLDAGERIGJEN));
        }

        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setFnr(fnr)
                .setAktoer_id(rs.getString(OPPFOLGING_DATA_AKTOERID))
                .setProfilering_resultat(Optional.ofNullable(rs.getString(BRUKER_PROFILERING_PROFILERING_RESULTAT)).map(Profileringsresultat::valueOf).orElse(null))
                .setUtdanning(rs.getString(BRUKER_REGISTRERING_UTDANNING))
                .setUtdanning_bestatt(rs.getString(BRUKER_REGISTRERING_UTDANNING_BESTATT))
                .setUtdanning_godkjent(rs.getString(BRUKER_REGISTRERING_UTDANNING_GODKJENT))
                .setUtdanning_og_situasjon_sist_endret(toLocalDate(rs.getTimestamp(BRUKER_REGISTRERING_REGISTRERING_OPPRETTET)))
                .setHar_delt_cv(rs.getBoolean(BRUKER_CV_HAR_DELT_CV))
                .setCv_eksistere(rs.getBoolean(BRUKER_CV_CV_EKSISTERER))
                .setOppfolging(rs.getBoolean(OPPFOLGING_DATA_OPPFOLGING))
                .setNy_for_veileder(rs.getBoolean(OPPFOLGING_DATA_NY_FOR_VEILEDER))
                .setVeileder_id(rs.getString(OPPFOLGING_DATA_VEILEDERID))
                .setManuell_bruker(rs.getBoolean(OPPFOLGING_DATA_MANUELL) ? "MANUELL" : null)
                .setOppfolging_startdato(toIsoUTC(rs.getTimestamp(OPPFOLGING_DATA_STARTDATO)))
                .setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp(DIALOG_VENTER_PA_BRUKER)))
                .setVenterpasvarfranav(toIsoUTC(rs.getTimestamp(DIALOG_VENTER_PA_NAV)))
                .setUtkast_14a_status(Optional.ofNullable(utkast14aStatus)
                        .map(Kafka14aStatusendring.Status::valueOf)
                        .map(Kafka14aStatusendring::statusTilTekst)
                        .orElse(null))
                .setUtkast_14a_status_endret(toIsoUTC(rs.getTimestamp(UTKAST_14A_STATUS_ENDRET_TIDSPUNKT)))
                .setUtkast_14a_ansvarlig_veileder(rs.getString(UTKAST_14A_STATUS_ANSVARLIG_VEILDERNAVN))
                .setYtelse(rs.getString(YTELSE_STATUS_FOR_BRUKER_YTELSE))
                .setUtlopsdato(toIsoUTC(rs.getTimestamp(YTELSE_STATUS_FOR_BRUKER_UTLOPSDATO)))
                .setDagputlopuke(rs.getObject(YTELSE_STATUS_FOR_BRUKER_DAGPUTLOPUKE, Integer.class))
                .setPermutlopuke(rs.getObject(YTELSE_STATUS_FOR_BRUKER_PERMUTLOPUKE, Integer.class))
                .setAapmaxtiduke(rs.getObject(YTELSE_STATUS_FOR_BRUKER_AAPMAXTIDUKE, Integer.class))
                .setAapordinerutlopsdato(aapordinerutlopsdato)
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getObject(YTELSE_STATUS_FOR_BRUKER_AAPUNNTAKDAGERIGJEN, Integer.class)))
                .setFargekategori(rs.getString(FARGEKATEGORI_VERDI))
                .setFargekategori_enhetId(rs.getString(FARGEKATEGORI_ENHET_ID));

        setHuskelapp(bruker, rs);
        setBrukersSituasjon(bruker, rs);
        setAapKelvin(bruker, rs);

        String arbeidslisteTidspunkt = toIsoUTC(rs.getTimestamp(ARBEIDSLISTE_ENDRINGSTIDSPUNKT));
        if (arbeidslisteTidspunkt != null) {
            String fargekategoriFraFargekategoriTabell = rs.getString(FARGEKATEGORI_VERDI);
            String fargekategoriFraArbeidslisteTabell = rs.getString(ARBEIDSLISTE_KATEGORI);
            String resolvedFargekategori = fargekategoriFraFargekategoriTabell != null
                    ? ArbeidslisteMapper.mapFraFargekategoriTilKategori(fargekategoriFraFargekategoriTabell).name()
                    : fargekategoriFraArbeidslisteTabell;

            bruker.setArbeidsliste_aktiv(true)
                    .setArbeidsliste_endringstidspunkt(arbeidslisteTidspunkt)
                    .setArbeidsliste_frist(Optional.ofNullable(toIsoUTC(rs.getTimestamp(ARBEIDSLISTE_FRIST))).orElse(getFarInTheFutureDate()))
                    .setArbeidsliste_kategori(resolvedFargekategori)
                    .setArbeidsliste_sist_endret_av_veilederid(rs.getString(ARBEIDSLISTE_SIST_ENDRET_AV_VEILEDERIDENT))
                    .setNavkontor_for_arbeidsliste(rs.getString(ARBEIDSLISTE_NAV_KONTOR_FOR_ARBEIDSLISTE));
            String overskrift = rs.getString(ARBEIDSLISTE_OVERSKRIFT);

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
        flettInnOppfolgingsbruker(bruker, rs);

        Date foedsels_dato = rs.getDate(BRUKER_DATA_FOEDSELSDATO);
        if (foedsels_dato != null) {
            flettInnDataFraPDL(rs, bruker);
        } else if (isDevelopment().orElse(false)) {
            bruker.setFnr(null); // Midlertidig forsikring for at brukere i q1 aldri har ekte data. Fjernes sammen med toggles, og bruk av inner join for brukerdata
        }
        bruker.setEgen_ansatt(rs.getBoolean(NOM_SKJERMING_ER_SKJERMET));
        bruker.setSkjermet_til(toLocalDateTimeOrNull(rs.getTimestamp(NOM_SKJERMING_SKJERMET_TIL)));

        return bruker;
    }

    @SneakyThrows
    private void setAapKelvin(OppfolgingsBruker oppfolgingsBruker, ResultSet rs) {
        boolean harStatusLøpende = rs.getString(YTELSER_AAP_STATUS) != null && rs.getString(YTELSER_AAP_STATUS).equals("LØPENDE");
        boolean tomDatoErEtterDagensDato = rs.getDate(YTELSER_AAP_NYESTE_PERIODE_TOM) != null && rs.getDate(YTELSER_AAP_NYESTE_PERIODE_TOM).toLocalDate().isAfter(LocalDate.now());
        oppfolgingsBruker.setAap_kelvin(harStatusLøpende && tomDatoErEtterDagensDato);
    }

    @SneakyThrows
    private void setHuskelapp(OppfolgingsBruker oppfolgingsBruker, ResultSet rs) {
        LocalDate frist = toLocalDate(rs.getTimestamp(HUSKELAPP_FRIST));
        String kommentar = rs.getString(HUSKELAPP_KOMMENTAR);
        String huskelappId = rs.getString(HUSKELAPP_HUSKELAPP_ID);
        LocalDate endretDato = toLocalDate(rs.getTimestamp(HUSKELAPP_ENDRET_DATO));
        VeilederId endretAv = VeilederId.veilederIdOrNull(rs.getString(HUSKELAPP_ENDRET_AV_VEILEDER));
        String enhetId = rs.getString(HUSKELAPP_ENHET_ID);
        if (frist != null || kommentar != null) {
            oppfolgingsBruker.setHuskelapp(new HuskelappForBruker(frist, kommentar, endretDato, endretAv.getValue(), huskelappId, enhetId));
        }
    }

    @SneakyThrows
    private void setBrukersSituasjon(OppfolgingsBruker oppfolgingsBruker, ResultSet rs) {
        boolean harOppdatertBrukersSituasjon = rs.getString(ENDRING_I_REGISTRERING_BRUKERS_SITUASJON) != null && rs.getTimestamp(ENDRING_I_REGISTRERING_BRUKERS_SITUASJON_SIST_ENDRET) != null;
        LocalDate oppdatertBrukesSituasjonSistEndretDato = toLocalDate(rs.getTimestamp(ENDRING_I_REGISTRERING_BRUKERS_SITUASJON_SIST_ENDRET));
        LocalDate brukesSituasjonOpprettetDato = toLocalDate(rs.getTimestamp(BRUKER_REGISTRERING_REGISTRERING_OPPRETTET));
        boolean harEndretSituasjonEttterRegistrering = harOppdatertBrukersSituasjon && isEqualOrAfterWithNullCheck(oppdatertBrukesSituasjonSistEndretDato, brukesSituasjonOpprettetDato);
        String brukersSisteSituasjon = harEndretSituasjonEttterRegistrering ? rs.getString(ENDRING_I_REGISTRERING_BRUKERS_SITUASJON) : rs.getString(BRUKER_REGISTRERING_BRUKERS_SITUASJON);
        LocalDate brukersSituasjonSistEndretDato = harEndretSituasjonEttterRegistrering ? oppdatertBrukesSituasjonSistEndretDato : brukesSituasjonOpprettetDato;

        oppfolgingsBruker.setBrukers_situasjoner(brukersSisteSituasjon == null ? emptyList() : List.of(brukersSisteSituasjon));
        oppfolgingsBruker.setBrukers_situasjon_sist_endret(brukersSituasjonSistEndretDato);
    }

    @SneakyThrows
    private OppfolgingsBruker flettInnOppfolgingsbruker(OppfolgingsBruker bruker, ResultSet rs) {
        String fnr = rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_FODSELSNR);
        if (fnr == null) {
            return bruker;
        }

        String formidlingsgruppekode = rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_FORMIDLINGSGRUPPEKODE);
        String kvalifiseringsgruppekode = rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE);
        return bruker
                .setFnr(fnr)
                .setEnhet_id(rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_NAV_KONTOR))
                .setIserv_fra_dato(toIsoUTC(rs.getTimestamp(OPPFOLGINGSBRUKER_ARENA_V2_ISERV_FRA_DATO)))
                .setRettighetsgruppekode(rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_RETTIGHETSGRUPPEKODE))
                .setHovedmaalkode(rs.getString(OPPFOLGINGSBRUKER_ARENA_V2_HOVEDMAALKODE))
                .setFormidlingsgruppekode(formidlingsgruppekode)
                .setKvalifiseringsgruppekode(kvalifiseringsgruppekode)
                .setTrenger_vurdering(OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setEr_sykmeldt_med_arbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode));
    }

    @SneakyThrows
    private void flettInnDataFraPDL(ResultSet rs, OppfolgingsBruker bruker) {
        Date foedselsdato = rs.getDate(BRUKER_DATA_FOEDSELSDATO);
        String mellomnavn = rs.getString(BRUKER_DATA_MELLOMNAVN);
        String fornavn = rs.getString(BRUKER_DATA_FORNAVN);
        if (mellomnavn != null) {
            fornavn += " " + mellomnavn;
        }
        String etternavn = rs.getString(BRUKER_DATA_ETTERNAVN);

        String foedeland = rs.getString(BRUKER_DATA_FOEDELAND);
        String landGruppe = Landgruppe.getInstance().getLandgruppeForLandKode(foedeland);
        String foedelandFulltNavn = kodeverkService.getBeskrivelseForLandkode(foedeland);

        Date sikkerhetstiltakGyldigtil = rs.getDate(BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGTIL);
        boolean showSikkerhetsTiltak = (sikkerhetstiltakGyldigtil == null || sikkerhetstiltakGyldigtil.toLocalDate().isAfter(LocalDate.now()));

        bruker
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", etternavn, fornavn))
                .setEr_doed(rs.getBoolean(BRUKER_DATA_ER_DOED))
                .setFodselsdag_i_mnd(foedselsdato.toLocalDate().getDayOfMonth())
                .setFodselsdato(lagFodselsdato(foedselsdato.toLocalDate()))
                .setFoedeland(foedeland)
                .setFoedelandFulltNavn(foedelandFulltNavn)
                .setKjonn(rs.getString(BRUKER_DATA_KJOENN))
                .setTalespraaktolk(rs.getString(BRUKER_DATA_TALESPRAAKTOLK))
                .setTegnspraaktolk(rs.getString(BRUKER_DATA_TEGNSPRAAKTOLK))
                .setTolkBehovSistOppdatert(DateUtils.toLocalDateOrNull(rs.getString(BRUKER_DATA_TOLKBEHOVSISTOPPDATERT)))
                .setLandgruppe((landGruppe != null && !landGruppe.isEmpty()) ? landGruppe : null)
                .setBydelsnummer(rs.getString(BRUKER_DATA_BYDELSNUMMER))
                .setKommunenummer(rs.getString(BRUKER_DATA_KOMMUNENUMMER))
                .setUtenlandskAdresse(rs.getString(BRUKER_DATA_UTENLANDSKADRESSE))
                .setHarUkjentBosted(rs.getBoolean(BRUKER_DATA_HARUKJENTBOSTED))
                .setBostedSistOppdatert(toLocalDateOrNull(rs.getString(BRUKER_DATA_BOSTEDSISTOPPDATERT)))
                .setSikkerhetstiltak(showSikkerhetsTiltak ? rs.getString(BRUKER_DATA_SIKKERHETSTILTAK_TYPE) : null)
                .setSikkerhetstiltak_gyldig_fra(showSikkerhetsTiltak ? rs.getString(BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGFRA) : null)
                .setSikkerhetstiltak_gyldig_til(showSikkerhetsTiltak ? rs.getString(BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGTIL) : null)
                .setSikkerhetstiltak_beskrivelse(showSikkerhetsTiltak ? rs.getString(BRUKER_DATA_SIKKERHETSTILTAK_BESKRIVELSE) : null)
                .setDiskresjonskode(rs.getString(BRUKER_DATA_DISKRESJONKODE));
    }
}
