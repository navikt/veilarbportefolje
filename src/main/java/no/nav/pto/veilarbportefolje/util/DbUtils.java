package no.nav.pto.veilarbportefolje.util;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;

import javax.sql.DataSource;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

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
        config.setConnectionTimeout(600000); // 10min
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

    public static String boolToJaNei(boolean bool) {
        return bool ? "J" : "N";
    }
}
