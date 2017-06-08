package no.nav.fo.mock;

import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*;

public class AktoerMock implements AktoerV2 {
    @Override
    public WSHentAktoerIdForIdentListeResponse hentAktoerIdForIdentListe(WSHentAktoerIdForIdentListeRequest wsHentAktoerIdForIdentListeRequest) {
        return null;
    }

    @Override
    public WSHentAktoerIdForIdentResponse hentAktoerIdForIdent(WSHentAktoerIdForIdentRequest wsHentAktoerIdForIdentRequest) throws HentAktoerIdForIdentPersonIkkeFunnet {
        return null;
    }

    @Override
    public WSHentIdentForAktoerIdListeResponse hentIdentForAktoerIdListe(WSHentIdentForAktoerIdListeRequest wsHentIdentForAktoerIdListeRequest) {
        return null;
    }

    @Override
    public void ping() {

    }

    @Override
    public WSHentIdentForAktoerIdResponse hentIdentForAktoerId(WSHentIdentForAktoerIdRequest wsHentIdentForAktoerIdRequest) throws HentIdentForAktoerIdPersonIkkeFunnet {
        String tulleId = "01010101010";
        WSHentIdentForAktoerIdResponse response = new WSHentIdentForAktoerIdResponse();
        response.setIdent(tulleId);
        return response;
    }
}
