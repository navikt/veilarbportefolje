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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseUtils.konverterDagerTilUker;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukNOMSkjerming;
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

@Slf4j
@Repository
@RequiredArgsConstructor
public class BrukerRepositoryV2 {
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;
    private final UnleashService unleashService;

    public OppfolgingsBruker hentOppfolgingsBruker(AktorId aktorId) {
        return hentOppfolgingsBrukere(List.of(aktorId))
                .stream().findAny().orElse(null);
    }

    /*
    NOTE: hvis oppfolgingsbruker_arena_v2 ikke lenger er kritisk
    Bytt ut inner join med left join
     */
    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds) {
        List<OppfolgingsBruker> result = new ArrayList<>();

        var params = new MapSqlParameterSource(
                "aktorIds",
                aktorIds.stream().map(AktorId::get) .collect(Collectors.joining(",", "{", "}"))
        );
        return namedDb.query("""
                        SELECT ad.*, ob.*, ns.er_skjermet, ai.fnr
                        from aktorid_indeksert_data ad
                        inner join aktive_identer ai on ad.aktoerid = ai.aktorid
                        inner join oppfolgingsbruker_arena_v2 ob on ob.fodselsnr = ai.fnr
                        left join nom_skjerming ns on ns.fodselsnr = ai.fnr
                        where aktoerid = ANY (:aktorIds::varchar[])
                        """,
                params, (ResultSet rs) -> {
                    while (rs.next()) {
                        result.add(mapTilOppfolgingsBruker(rs));
                    }
                    return result;
                });
    }

    @SneakyThrows
    private OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs) {
        String formidlingsgruppekode = rs.getString(FORMIDLINGSGRUPPEKODE);
        String kvalifiseringsgruppekode = rs.getString(KVALIFISERINGSGRUPPEKODE);

        String fnr = rs.getString(FODSELSNR);
        String fornavn = rs.getString(FORNAVN);
        String etternavn = rs.getString(ETTERNAVN);
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
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getObject(AAPUNNTAKDAGERIGJEN, Integer.class)))

                .setFodselsdag_i_mnd(Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(fnr)))
                .setFodselsdato(FodselsnummerUtils.lagFodselsdato(fnr))
                .setKjonn(FodselsnummerUtils.lagKjonn(fnr));

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

        // ARENA DB LENKE: skal fjernes på sikt
        return bruker
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", fornavn, etternavn))
                .setEnhet_id(rs.getString(NAV_KONTOR))
                .setIserv_fra_dato(toIsoUTC(rs.getTimestamp(ISERV_FRA_DATO)))
                .setRettighetsgruppekode(rs.getString(RETTIGHETSGRUPPEKODE))
                .setHovedmaalkode(rs.getString(HOVEDMAALKODE))
                .setSikkerhetstiltak(rs.getString(SIKKERHETSTILTAK_TYPE_KODE))
                .setDiskresjonskode(rs.getString(DISKRESJONSKODE))
                .setEr_doed(rs.getBoolean(ER_DOED))
                .setFormidlingsgruppekode(formidlingsgruppekode)
                .setKvalifiseringsgruppekode(kvalifiseringsgruppekode)
                .setTrenger_vurdering(OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setEr_sykmeldt_med_arbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, vedtakstatus));
    }
}
