import no.nav.pto.veilarbportefolje.util.PostgresContainer;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;

import java.io.IOException;

import static no.nav.pto.veilarbportefolje.util.PostgresContainer.DB_USER;

public class StartLokalDatabase {
    public static void main(String[] args) throws IOException {
        PostgresContainer container = SingletonPostgresContainer.init();
        System.out.println("Lokal database kan kobles til med bruker " + DB_USER + " og url " + container.getDbContainerUrl());
        System.out.println("Trykk enter for å avslutte");
        System.in.read();
    }
}
