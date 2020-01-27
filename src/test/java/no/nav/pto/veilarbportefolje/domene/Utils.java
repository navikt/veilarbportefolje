package no.nav.pto.veilarbportefolje.domene;

import no.nav.virksomhet.organisering.enhetogressurs.v1.Ressurs;

public class Utils {

    public static Ressurs createRessurs() {
            Ressurs ressurs = new Ressurs();
            ressurs.setRessursId("ressurs id");
            ressurs.setNavn("fornavn etternavn");
            ressurs.setEtternavn("etternavn");
            ressurs.setFornavn("fornavn");
            return ressurs;

    }
}
