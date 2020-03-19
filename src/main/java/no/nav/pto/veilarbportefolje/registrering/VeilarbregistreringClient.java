package no.nav.pto.veilarbportefolje.registrering;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.registrering.domene.BrukerRegistreringWrapper;
import no.nav.sbl.util.EnvironmentUtils;

import javax.ws.rs.client.Client;


@Slf4j
public class VeilarbregistreringClient {

    private static final String VEILARBREGISTRERING_URL = EnvironmentUtils.getRequiredProperty("VEILARBREGISTRERING_URL");
    private Client client;


    public VeilarbregistreringClient (Client client) {
        this.client = client;
    }
    public Try<BrukerRegistreringWrapper> hentRegistrering(Fnr fnr) {
        return Try.of(() -> client.target(String.join(VEILARBREGISTRERING_URL, "veilarbregistrering/api/", "/registrering?fnr=", fnr.toString())).request().get())
                .filter((resp) -> resp.getStatus() >= 200 && resp.getStatus() < 300)
                .map((resp) -> resp.getStatus() == 204 ? null : resp.readEntity(BrukerRegistreringWrapper.class))
                .onFailure(error -> log.warn(String.format("Feilede att med føljande fel : %s ", error)));
    }
}
