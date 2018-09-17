package no.nav.fo.smoketest;

import lombok.extern.slf4j.Slf4j;
import no.nav.dialogarena.smoketest.SmoketestFSS;
import no.nav.fo.domene.EnhetTiltak;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Portefolje;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import java.util.List;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.smoketest.SmoketestUtils.appOrLocalhost;
import static no.nav.fo.config.CacheConfig.VEILARBVEILEDER;
import static no.nav.sbl.rest.RestUtils.withClient;

@Slf4j
@Tag("smoketest")
public class PortefoljeSmoketest {

    private static SmoketestFSS smoketestFSS;
    private static String PORTEFOLJE_URL;
    private static String PORTEFOLJE_VEILEDER_URL;
    private static String TILTAK_URL;
    private static String HOSTNAME;
    private static List<String> enhetsliste;

    @BeforeAll
    public static void setup() {
        HOSTNAME = appOrLocalhost(getProperty("miljo"));
        SmoketestFSS.SmoketestFSSConfig config = new SmoketestFSS.SmoketestFSSConfig(VEILARBVEILEDER);
        smoketestFSS = new SmoketestFSS(config);
        enhetsliste = SmoketestUtils.getEnheterForVeileder(smoketestFSS);
        configureUrls();
    }

    @Test
    public void hentPortefoljeForAlleTilgjengeligeEnhter() {
        enhetsliste.forEach(this::hentMax100BrukereForEnhet);
    }

    @Test
    public void hentPortefoljeForVeilederForAlleTilgjengeligeEnheter() {
        enhetsliste.forEach(enhet -> hentMax100BrukereForVeileder(enhet, smoketestFSS.getInnloggetVeielder()));
    }

    @Test
    public void hentTiltakForAlleEnheter() {
        enhetsliste.forEach(this::hentTiltakForEnhet);
    }

    private EnhetTiltak hentTiltakForEnhet(String enhet) {
        log.info("Henter tiltak for enhet {}", enhet);
        return withClient(client -> client.target(String.format(TILTAK_URL, enhet))
                .request()
                .cookie(smoketestFSS.getTokenCookie())
                .get(EnhetTiltak.class));
    }

    private Portefolje hentMax100BrukereForEnhet(String enhet) {
        log.info("Henter portefolje for enhet {}", enhet);
        return withClient(client -> client.target(String.format(PORTEFOLJE_URL, enhet,100))
                .request()
                .cookie(smoketestFSS.getTokenCookie())
                .post(Entity.json(new Filtervalg()),Portefolje.class));
    }

    private Portefolje hentMax100BrukereForVeileder(String enhet, String veileder) {
        log.info("Henter portefolje for veileder {} for enhet {}",veileder, enhet);
        return withClient(client -> client.target(String.format(PORTEFOLJE_VEILEDER_URL,veileder, enhet,100))
                .request()
                .cookie(smoketestFSS.getTokenCookie())
                .post(Entity.json(new Filtervalg()),Portefolje.class));
    }


    private static void configureUrls() {
        PORTEFOLJE_URL = HOSTNAME + "veilarbportefolje/api/enhet/%s/portefolje" +
                "?fra=0&antall=%s&sortDirection=ikke_satt&sortField=ikke_satt";
        PORTEFOLJE_VEILEDER_URL = HOSTNAME + "veilarbportefolje/api/veileder/%s/portefolje" +
                "?enhet=%s&fra=0&antall=%s&sortDirection=ikke_satt&sortField=ikke_satt";
        TILTAK_URL = HOSTNAME + "veilarbportefolje/api/enhet/%s/tiltak";
    }
}
