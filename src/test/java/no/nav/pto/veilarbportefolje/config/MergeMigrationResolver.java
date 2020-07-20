package no.nav.pto.veilarbportefolje.config;

import lombok.SneakyThrows;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.resolver.Context;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.resolver.ResolvedMigrationComparator;
import org.flywaydb.core.internal.resolver.sql.SqlMigrationResolver;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.sqlscript.SqlScriptExecutorFactory;
import org.flywaydb.core.internal.sqlscript.SqlScriptFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MergeMigrationResolver implements MigrationResolver {
    private ResolvedMigrationComparator comparator = new ResolvedMigrationComparator();
    private String[] mergeLocations = new String[]{"testmigration", "db/migration"};



    private List<ResolvedMigration> mergeDuplicateMigrations(List<ResolvedMigration> migrations){
        List<ResolvedMigration> newMigrationsList = new ArrayList<>();

        for (ResolvedMigration migration: migrations){
            if (!existsAllreadyInList(newMigrationsList, migration)){
                newMigrationsList.add(migration);
            }
        }

        return newMigrationsList;
    }

    private boolean existsAllreadyInList(List<ResolvedMigration> migrations, ResolvedMigration migration){
        for (ResolvedMigration possibleDuplicate: migrations){
            if (comparator.compare(possibleDuplicate, migration) == 0){
                return true;
            }
        }
        return false;
    }


    public Collection<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();
        /*
        for (String locationStr : mergeLocations) {
            Location location = new Location(locationStr);
            SqlMigrationResolver res = new SqlMigrationResolver(
                    context.getConfiguration().getResourceProvider(),
                    context.getConfiguration()
           /*SqlMigrationResolver res = new SqlMigrationResolver(dbSupport, scanner, location, placeholderReplacer,
                    "UTF-8", "V", "R", "__", ".sql");
            migrations.addAll(res.resolveMigrations());
            }
         */


        return mergeDuplicateMigrations(migrations);
    }
}
