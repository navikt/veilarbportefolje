package no.nav.pto.veilarbportefolje.util;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.CV_EKSISTERE;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.HAR_DELT_CV;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.OppfolgingUtils.isNyForEnhet;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
public class DbUtils {
   private enum DbRole {
        ADMIN,
        READONLY,
    }

    public static DataSource createDataSource(String dbUrl, boolean admin) {
        HikariConfig config = createDataSourceConfig(dbUrl);
        if(admin){
            return createVaultRefreshDataSource(config, DbRole.ADMIN);
        }
        return createVaultRefreshDataSource(config, DbRole.READONLY);
    }

    public static HikariConfig createDataSourceConfig(String dbUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        return config;
    }

    public static String getSqlAdminRole() {
        boolean isProd = isProduction().orElse(false);
        return (isProd ? "veilarbportefolje-prod-admin" : "veilarbportefolje-dev-admin");
    }

    public static String getSqlReadOnlyRole() {
        boolean isProd = isProduction().orElse(false);
        return (isProd ? "veilarbportefolje-prod-readonly" : "veilarbportefolje-dev-readonly");
    }

    @SneakyThrows
    private static DataSource createVaultRefreshDataSource(HikariConfig config, DbRole role) {
       if(role.equals(DbRole.READONLY)){
           return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), getSqlReadOnlyRole());
       }
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), getSqlAdminRole());
    }

    private static String getMountPath() {
        boolean isProd = isProduction().orElse(false);
        return "postgresql/" + (isProd ? "prod-fss" : "preprod-fss");
    }

    /***
     Oracle
     ***/
    @SneakyThrows
    public static OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs) {
        String formidlingsgruppekode = rs.getString("formidlingsgruppekode");
        String kvalifiseringsgruppekode = rs.getString("kvalifiseringsgruppekode");
        String brukersSituasjon = rs.getString("BRUKERS_SITUASJON");

        String fornavn = rs.getString("fornavn");
        String etternavn = rs.getString("etternavn");
        String vedtakstatus = rs.getString("VEDTAKSTATUS");
        String ansvarligVeilederForVedtak = rs.getString("ANSVARLIG_VEILEDER_NAVN");

        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setPerson_id(numberToString(rs.getBigDecimal("person_id")))
                .setOppfolging_startdato(toIsoUTC(rs.getTimestamp("oppfolging_startdato")))
                .setAktoer_id(rs.getString("aktoerid"))
                .setFnr(rs.getString("fodselsnr"))
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", etternavn, fornavn))
                .setEnhet_id(rs.getString("nav_kontor"))
                .setFormidlingsgruppekode(formidlingsgruppekode)
                .setIserv_fra_dato(toIsoUTC(rs.getTimestamp("iserv_fra_dato")))
                .setKvalifiseringsgruppekode(kvalifiseringsgruppekode)
                .setRettighetsgruppekode(rs.getString("rettighetsgruppekode"))
                .setHovedmaalkode(rs.getString("hovedmaalkode"))
                .setSikkerhetstiltak(rs.getString("sikkerhetstiltak_type_kode"))
                .setDiskresjonskode(rs.getString("fr_kode"))
                .setEgen_ansatt(parseJaNei(rs.getString("sperret_ansatt"), "sperret_ansatt"))
                .setEr_doed(parseJaNei(rs.getString("er_doed"), "er_doed"))
                .setDoed_fra_dato(toIsoUTC(rs.getTimestamp("doed_fra_dato")))
                .setVeileder_id(rs.getString("veilederident"))
                .setFodselsdag_i_mnd(Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(rs.getString("fodselsnr"))))
                .setFodselsdato(FodselsnummerUtils.lagFodselsdato(rs.getString("fodselsnr")))
                .setKjonn(FodselsnummerUtils.lagKjonn(rs.getString("fodselsnr")))
                .setYtelse(rs.getString("ytelse"))
                .setUtlopsdato(toIsoUTC(rs.getTimestamp("utlopsdato")))
                .setUtlopsdatofasett(rs.getString("utlopsdatofasett"))
                .setDagputlopuke(parseInt(rs.getString("dagputlopuke")))
                .setDagputlopukefasett(rs.getString("dagputlopukefasett"))
                .setPermutlopuke(parseInt(rs.getString("permutlopuke")))
                .setAapmaxtiduke(parseInt(rs.getString("aapmaxtiduke")))
                .setAapmaxtidukefasett(rs.getString("aapmaxtidukefasett"))
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getString("aapunntakdagerigjen")))
                .setAapunntakukerigjenfasett(rs.getString("aapunntakukerigjenfasett"))
                .setOppfolging(parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING"))
                .setNy_for_veileder(parseJaNei(rs.getString("NY_FOR_VEILEDER"), "NY_FOR_VEILEDER"))
                .setNy_for_enhet(isNyForEnhet(rs.getString("veilederident")))
                .setTrenger_vurdering(OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp("venterpasvarfrabruker")))
                .setVenterpasvarfranav(toIsoUTC(rs.getTimestamp("venterpasvarfranav")))
                .setNyesteutlopteaktivitet(toIsoUTC(rs.getTimestamp("nyesteutlopteaktivitet")))
                .setAktivitet_start(toIsoUTC(rs.getTimestamp("aktivitet_start")))
                .setNeste_aktivitet_start(toIsoUTC(rs.getTimestamp("neste_aktivitet_start")))
                .setForrige_aktivitet_start(toIsoUTC(rs.getTimestamp("forrige_aktivitet_start")))
                .setManuell_bruker(identifiserManuellEllerKRRBruker(rs.getString("RESERVERTIKRR"), rs.getString("MANUELL")))
                .setBrukers_situasjon(brukersSituasjon)
                .setEr_sykmeldt_med_arbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setVedtak_status(Optional.ofNullable(vedtakstatus).map(KafkaVedtakStatusEndring.VedtakStatusEndring::valueOf).map(KafkaVedtakStatusEndring::vedtakStatusTilTekst).orElse(null))
                .setVedtak_status_endret(toIsoUTC(rs.getTimestamp("VEDTAK_STATUS_ENDRET_TIDSPUNKT")))
                .setAnsvarlig_veileder_for_vedtak(ansvarligVeilederForVedtak)
                .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, vedtakstatus))
                .setProfilering_resultat(rs.getString("profilering_resultat"))
                .setHar_delt_cv(parseJaNei(rs.getString(HAR_DELT_CV), HAR_DELT_CV))
                .setCv_eksistere(parseJaNei(rs.getString(CV_EKSISTERE), CV_EKSISTERE))
                .setUtdanning(rs.getString("UTDANNING"))
                .setUtdanning_bestatt(rs.getString("UTDANNING_BESTATT"))
                .setUtdanning_godkjent(rs.getString("UTDANNING_GODKJENT"));


        boolean brukerHarArbeidsliste = parseJaNei(rs.getString("ARBEIDSLISTE_AKTIV"), "ARBEIDSLISTE_AKTIV");

        if (brukerHarArbeidsliste) {
            bruker
                    .setArbeidsliste_aktiv(true)
                    .setArbeidsliste_sist_endret_av_veilederid(rs.getString("ARBEIDSLISTE_ENDRET_AV"))
                    .setArbeidsliste_endringstidspunkt(toIsoUTC(rs.getTimestamp("ARBEIDSLISTE_ENDRET_TID")))
                    .setArbeidsliste_kommentar(rs.getString("ARBEIDSLISTE_KOMMENTAR"))
                    .setArbeidsliste_overskrift(rs.getString("ARBEIDSLISTE_OVERSKRIFT"))
                    .setArbeidsliste_kategori(rs.getString("ARBEIDSLISTE_KATEGORI"))
                    .setArbeidsliste_frist(Optional.ofNullable(toIsoUTC(rs.getTimestamp("ARBEIDSLISTE_FRIST"))).orElse(getFarInTheFutureDate()));
        }

        return bruker;
    }

    public static Integer konverterDagerTilUker(String antallDagerFraDB) {
        Integer antallDager = parseInt(antallDagerFraDB);
        return antallDager == null ? 0 : (antallDager / 5);
    }

    public static String identifiserManuellEllerKRRBruker(String krrJaNei, String manuellJaNei) {
        if ("J".equals(krrJaNei)) {
            return "KRR";
        } else if ("J".equals(manuellJaNei)) {
            return "MANUELL";
        }
        return null;
    }

    public static boolean parseJaNei(Object janei, String name) {
        boolean defaultValue = false;
        if (janei == null) {
            log.debug(String.format("%s er ikke satt i databasen, defaulter til %b", name, defaultValue));
            return defaultValue;
        }

        switch (janei.toString()) {
            case "J":
                return true;
            case "N":
                return false;
            default:
                throw new IllegalArgumentException(String.format("Kunne ikke parse verdi %s fra database til boolean", janei));
        }
    }

    static Integer parseInt(String integer) {
        if (integer == null) {
            return null;
        }
        return Integer.parseInt(integer);
    }

    public static Boolean parse0OR1(String value) {
        if (value == null) {
            return null;
        }
        return "1".equals(value);
    }

    public static String boolTo0OR1(boolean bool) {
        return bool ? "1" : "0";
    }

    public static String boolToJaNei(boolean bool) {
        return bool ? "J" : "N";
    }

    public static String numberToString(BigDecimal bd) {
        return String.valueOf(bd.intValue());
    }

    public static <S> Set<S> toSet(S s) {
        Set<S> set = new HashSet<>();
        set.add(s);
        return set;
    }


    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return (T t) -> !predicate.test(t);
    }
}
