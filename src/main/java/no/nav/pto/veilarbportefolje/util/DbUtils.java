package no.nav.pto.veilarbportefolje.util;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.function.Predicate;

import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
public class DbUtils {
    private enum DbRole {
        ADMIN,
        READONLY,
    }

    public static DataSource createDataSource(String dbUrl, boolean admin) {
        if (admin) {
            HikariConfig config = createDataSourceConfig(dbUrl, 2);
            return createVaultRefreshDataSource(config, DbRole.ADMIN);
        }
        HikariConfig config = createDataSourceConfig(dbUrl, 3);
        return createVaultRefreshDataSource(config, DbRole.READONLY);
    }

    public static HikariConfig createDataSourceConfig(String dbUrl, int maximumPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maximumPoolSize);
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
        if (role.equals(DbRole.READONLY)) {
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
    public static OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs, UnleashService unleashService) {
        String formidlingsgruppekode = rs.getString("formidlingsgruppekode");
        String kvalifiseringsgruppekode = rs.getString("kvalifiseringsgruppekode");

        String fornavn = rs.getString("fornavn");
        String etternavn = rs.getString("etternavn");
        String vedtakstatus = rs.getString("VEDTAKSTATUS");

        OppfolgingsBruker bruker = new OppfolgingsBruker()
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
                .setFodselsdag_i_mnd(Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(rs.getString("fodselsnr"))))
                .setFodselsdato(FodselsnummerUtils.lagFodselsdato(rs.getString("fodselsnr")))
                .setKjonn(FodselsnummerUtils.lagKjonn(rs.getString("fodselsnr")))
                .setTrenger_vurdering(OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setEr_sykmeldt_med_arbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, vedtakstatus))
                .setOppfolging(parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING")); // Oppfolging hentes fra Oracle helt til at alt er migrert
        return bruker;
    }

    public static boolean parseJaNei(Object janei, String name) {
        boolean defaultValue = false;
        if (janei == null) {
            log.debug(String.format("%s er ikke satt i databasen, defaulter til %b", name, defaultValue));
            return defaultValue;
        }

        return switch (janei.toString()) {
            case "J" -> true;
            case "N" -> false;
            default -> throw new IllegalArgumentException(String.format("Kunne ikke parse verdi %s fra database til boolean", janei));
        };
    }

    public static String boolToJaNei(boolean bool) {
        return bool ? "J" : "N";
    }


    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return (T t) -> !predicate.test(t);
    }
}
