package no.nav.fo.mock;

import no.nav.virksomhet.organisering.enhetogressurs.v1.Ressurs;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.*;
import no.nav.virksomhet.tjenester.enhet.v1.*;

import java.util.ArrayList;
import java.util.List;

public class EnhetMock implements Enhet {

    public static final String NAV_SANDE_ID = "0713";

    @Override
    public WSFinnEnhetListeResponse finnEnhetListe(WSFinnEnhetListeRequest request) {
        return null;
    }

    @Override
    public void ping() {

    }

    @Override
    public WSHentRessursListeResponse hentRessursListe(WSHentRessursListeRequest request) throws HentRessursListeUgyldigInput, HentRessursListeEnhetikkefunnet {
        no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet enhet = new no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet().withEnhetId(request.getEnhetId());
        List<Ressurs> ressursliste = new ArrayList<>();
        ressursliste.add(createRessurs("Arne","And","X111111"));
        ressursliste.add(createRessurs("Jens Bjarne","Olsen","X222222"));
        ressursliste.add(createRessurs("Donald","Duck","X333333"));

        return new WSHentRessursListeResponse().withEnhet(enhet).withRessursListe(ressursliste);
    }

    @Override
    public WSHentEnhetListeResponse hentEnhetListe(WSHentEnhetListeRequest request) throws HentEnhetListeUgyldigInput, HentEnhetListeRessursIkkeFunnet {
        Ressurs ressurs = createRessurs("Arne","And",request.getRessursId());
        List<no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet> enhetliste = new ArrayList<>();
        enhetliste.add(createEnhet(NAV_SANDE_ID,"NAV SANDE"));
        enhetliste.add(createEnhet("0104","NAV MOSS"));
        enhetliste.add(createEnhet("0100","NAV ØSTFOLD"));
        enhetliste.add(createEnhet("0709","NAV LARVIK"));

        return new WSHentEnhetListeResponse().withEnhetListe(enhetliste).withRessurs(ressurs);
    }

    private Ressurs createRessurs(String fornavn, String etternavn, String ressursId) {
        return new Ressurs()
                .withFornavn(fornavn)
                .withEtternavn(etternavn)
                .withNavn(fornavn + " " + etternavn)
                .withRessursId(ressursId);
    }

    private no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet createEnhet(String enhetId, String enhetNavn) {
        return new no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet().withNavn(enhetNavn).withEnhetId(enhetId);
    }
}
