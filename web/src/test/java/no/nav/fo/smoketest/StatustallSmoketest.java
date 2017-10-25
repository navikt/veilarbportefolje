package no.nav.fo.smoketest;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.StatusTall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.nav.sbl.rest.RestUtils.withClient;

@Slf4j
@Tag("smoketest")
public class StatustallSmoketest extends Smoketest {


    private String STATUSTALL_URL;
    private String STATUSTALL_VEILEDER_URL;

    private static List<String> enhetsliste;

    @BeforeEach
    public void setup() {
        configureUrls();
        enhetsliste = SmoketestUtils.getEnheterForVeileder();
    }


    @Test
    public void hentStatustallForAlleTilgjengeligeEnheter() {
        enhetsliste.forEach(this::getStatustallForEnhet);
    }

    @Test
    public void hentStatustallForAlleTilgjengeligeEnheterForVeileder() {
        enhetsliste.forEach(enhet -> getStatustallForVeileder(INNLOGGET_VEILEDER, enhet));
    }


    private StatusTall getStatustallForVeileder(String veileder, String enhet) {
        log.info("Henter veielder {} sine statustall for enhet {}", veileder, enhet);
        return withClient(client -> client.target(String.format(STATUSTALL_VEILEDER_URL, veileder, enhet))
                .request()
                .cookie(tokenCookie)
                .get(StatusTall.class));
    }

    private StatusTall getStatustallForEnhet(String enhet) {
        log.info("Henter statustall for enhet {}", enhet);
        return withClient(client -> client.target(String.format(STATUSTALL_URL, enhet))
                .request()
                .cookie(tokenCookie)
                .get(StatusTall.class));
    }

    private void configureUrls() {
        STATUSTALL_URL = HOSTNAME + "veilarbportefolje/api/enhet/%s/statustall";
        STATUSTALL_VEILEDER_URL = HOSTNAME + "veilarbportefolje/api/veileder/%s/statustall?enhet=%s";
    }

}
