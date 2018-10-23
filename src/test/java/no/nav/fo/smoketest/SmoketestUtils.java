package no.nav.fo.smoketest;

import no.nav.dialogarena.smoketest.SmoketestException;
import no.nav.dialogarena.smoketest.SmoketestFSS;
import no.nav.fo.smoketest.domene.EnheterResponse;
import no.nav.fo.smoketest.domene.PortefoljeEnhet;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.getProperty;
import static no.nav.sbl.rest.RestUtils.withClient;

public class SmoketestUtils {

    private static String HENT_ENHETER_URL = no.nav.dialogarena.smoketest.SmoketestUtils.appOrLocalhost(getProperty("miljo")) + "veilarbveileder/api/veileder/enheter";

    public static List<String> getEnheterForVeileder(SmoketestFSS smoketest) {
        List<PortefoljeEnhet> enheter = withClient(client -> client.target(HENT_ENHETER_URL)
                .request()
                .cookie(smoketest.getTokenCookie())
                .get(EnheterResponse.class)).getEnhetliste();

        if (enheter.isEmpty()) {
            throw new SmoketestException("Fant ingen enheter i norg for veileder: " + smoketest.getInnloggetVeielder());
        }
        return enheter.stream().map(PortefoljeEnhet::getEnhetId).collect(Collectors.toList());
    }
}
