package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

@Component
public class HovedIndekseringHelsesjekk implements Helsesjekk {

    private static Exception indekseringFeilet;

    @Override
    public void helsesjekk() {
        if (indekseringFeilet != null) {
            throw new RuntimeException(indekseringFeilet);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "hovedindeksering",
                String.format("https://%s/%s", IndekseringConfig.getElasticHostname(), IndekseringConfig.ALIAS),
                "Sjekker om forrige hovedindeksering var vellykket",
                true
        );
    }

    public static void setIndekseringFeilet(Exception e) {
        indekseringFeilet = e;
    }

    public static void setIndekseringVellykket() {
        indekseringFeilet = null;
    }
}
