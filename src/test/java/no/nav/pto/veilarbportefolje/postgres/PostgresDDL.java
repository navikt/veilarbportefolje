package no.nav.pto.veilarbportefolje.postgres;

import no.nav.pto.veilarbportefolje.util.PostgresContainer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresDDL {
    private final String DDL_PATH = "./.intelliJ_ddl/DDL.sql";

    @Test
    public void kan_generere_postgres_DDL() throws IOException {
        var postgresContainer = new PostgresContainer();
        Flyway.configure()
                .dataSource(postgresContainer.createDataSource())
                .locations("db/postgres")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        PrintWriter writer = new PrintWriter(DDL_PATH);
        writer.print(postgresContainer.execInContainer("pg_dump", "-O", "-s", "-U", "postgres", "postgres"));
        writer.close();
        File file = new File(DDL_PATH);
        assertThat(file.isFile()).isTrue();
    }
}
