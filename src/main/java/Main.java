import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbportefolje.config.ApplicationConfig;

public class Main {

    public static void main(String... args) {
        ApiApp.runApp(ApplicationConfig.class, args);
    }

}
