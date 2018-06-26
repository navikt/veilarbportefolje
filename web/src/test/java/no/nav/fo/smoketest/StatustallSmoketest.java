package no.nav.fo.smoketest;

import lombok.extern.slf4j.Slf4j;
import no.nav.dialogarena.smoketest.SmoketestFSS;
import no.nav.fo.domene.StatusTall;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.smoketest.SmoketestUtils.appOrLocalhost;
import static no.nav.fo.config.CacheConfig.VEILARBVEILEDER;
import static no.nav.sbl.rest.RestUtils.withClient;

@Slf4j
@Tag("smoketest")
public class StatustallSmoketest {


    private static SmoketestFSS smoketestFSS;
    private static String STATUSTALL_URL;
    private static String STATUSTALL_VEILEDER_URL;

    private static List<String> enhetsliste;

    @BeforeAll
    public static void setup() {
        SmoketestFSS.SmoketestFSSConfig config = new SmoketestFSS.SmoketestFSSConfig(VEILARBVEILEDER);
        smoketestFSS = new SmoketestFSS(config);
        enhetsliste = SmoketestUtils.getEnheterForVeileder(smoketestFSS);
        configureUrls();
    }

    @Test
    public void hentStatustallForAlleTilgjengeligeEnheter() {
        enhetsliste.forEach(this::getStatustallForEnhet);
    }

    @Test
    public void hentStatustallForAlleTilgjengeligeEnheterForVeileder() {
        enhetsliste.forEach(enhet -> getStatustallForVeileder(smoketestFSS.getInnloggetVeielder(), enhet));
    }


    private StatusTall getStatustallForVeileder(String veileder, String enhet) {
        log.info("Henter veielder {} sine statustall for enhet {}", veileder, enhet);
        return withClient(client -> client.target(String.format(STATUSTALL_VEILEDER_URL, veileder, enhet))
                .request()
                .cookie(smoketestFSS.getTokenCookie())
                .get(StatusTall.class));
    }

    private StatusTall getStatustallForEnhet(String enhet) {
        log.info("Henter statustall for enhet {}", enhet);
        return withClient(client -> client.target(String.format(STATUSTALL_URL, enhet))
                .request()
                .cookie(smoketestFSS.getTokenCookie())
                .get(StatusTall.class));
    }

    private static void configureUrls() {
        STATUSTALL_URL = appOrLocalhost(getProperty("miljo")) + "veilarbportefolje/api/enhet/%s/statustall";
        STATUSTALL_VEILEDER_URL = appOrLocalhost(getProperty("miljo")) + "veilarbportefolje/api/veileder/%s/statustall?enhet=%s";
    }

}
