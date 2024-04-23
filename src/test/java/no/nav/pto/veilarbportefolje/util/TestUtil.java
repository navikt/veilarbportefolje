package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtil {

    public static void testMigrate(DataSource dataSource) {
        Flyway.configure()
                .validateMigrationNaming(true)
                .dataSource(dataSource)
                .locations("db/postgres")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    @SneakyThrows
    public static String readFileAsJsonString(String pathname, Class currentLocation) {
        var URI = currentLocation.getResource(pathname).toURI();
        var encodedBytes = Files.readAllBytes(Paths.get(URI));
        return new String(encodedBytes, UTF_8).trim();
    }

    @SneakyThrows
    public static String readTestResourceFile(String fileName) {
        URL fileUrl = TestUtil.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return Files.readString(resPath);
    }
}
