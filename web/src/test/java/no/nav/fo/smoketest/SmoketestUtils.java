package no.nav.fo.smoketest;

import no.nav.fo.smoketest.domene.EnheterResponse;
import no.nav.fo.smoketest.domene.PortefoljeEnhet;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.fo.smoketest.Smoketest.*;
import static no.nav.sbl.rest.RestUtils.withClient;

public class SmoketestUtils {

    private static String HENT_ENHETER_URL = HOSTNAME + "veilarbveileder/api/veileder/enheter";

    public static List<String> getEnheterForVeileder() {
        List<PortefoljeEnhet> enheter = withClient(client -> client.target(HENT_ENHETER_URL)
                .request()
                .cookie(tokenCookie)
                .get(EnheterResponse.class)).getEnhetliste();

        if (enheter.isEmpty()) {
            throw new SmoketestException("Fant ingen enheter i norg for veileder: " + INNLOGGET_VEILEDER);
        }
        return enheter.stream().map(PortefoljeEnhet::getEnhetId).collect(Collectors.toList());
    }
}
