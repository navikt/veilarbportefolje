package no.nav.fo.smoketest;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.EnhetTiltak;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Portefolje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import java.util.List;

import static no.nav.sbl.rest.RestUtils.withClient;

@Slf4j
@Tag("smoketest")
public class PortefoljeSmoketest extends Smoketest{

    private String PORTEFOLJE_URL;
    private String PORTEFOLJE_VEILEDER_URL;
    private String TILTAK_URL;
    private List<String> enhetsliste;


    @BeforeEach
    public void setup() {
        configureUrls();
        enhetsliste = SmoketestUtils.getEnheterForVeileder();
    }


    @Test
    public void hentPortefoljeForAlleTilgjengeligeEnhter() {
        enhetsliste.forEach(this::hentMax100BrukereForEnhet);
    }

    @Test
    public void hentPortefoljeForVeielderForAlleTilgjengeligeEnheter() {
        enhetsliste.forEach(enhet -> hentMax100BrukereForVeileder(enhet, INNLOGGET_VEILEDER));
    }

    @Test
    public void hentTiltakForAlleEnheter() {
        enhetsliste.forEach(this::hentTiltakForEnhet);
    }

    private EnhetTiltak hentTiltakForEnhet(String enhet) {
        log.info("Henter tiltak for enhet {}", enhet);
        return withClient(client -> client.target(String.format(TILTAK_URL, enhet))
                .request()
                .cookie(tokenCookie)
                .get(EnhetTiltak.class));
    }

    private Portefolje hentMax100BrukereForEnhet(String enhet) {
        log.info("Henter portefolje for enhet {}", enhet);
        return withClient(client -> client.target(String.format(PORTEFOLJE_URL, enhet,100))
                .request()
                .cookie(tokenCookie)
                .post(Entity.json(new Filtervalg()),Portefolje.class));
    }

    private Portefolje hentMax100BrukereForVeileder(String enhet, String veileder) {
        log.info("Henter portefolje for veileder {} for enhet {}",veileder, enhet);
        return withClient(client -> client.target(String.format(PORTEFOLJE_VEILEDER_URL,veileder, enhet,100))
                .request()
                .cookie(tokenCookie)
                .post(Entity.json(new Filtervalg()),Portefolje.class));
    }


    private void configureUrls() {
        PORTEFOLJE_URL = HOSTNAME + "veilarbportefolje/api/enhet/%s/portefolje" +
                "?fra=0&antall=%s&sortDirection=ikke_satt&sortField=ikke_satt";
        PORTEFOLJE_VEILEDER_URL = HOSTNAME + "veilarbportefolje/api/veileder/%s/portefolje" +
                "?enhet=%s&fra=0&antall=%s&sortDirection=ikke_satt&sortField=ikke_satt";
        TILTAK_URL = HOSTNAME + "veilarbportefolje/api/enhet/%s/tiltak";
    }
}
