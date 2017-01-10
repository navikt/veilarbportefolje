package no.nav.fo.domene;

import no.nav.virksomhet.organisering.enhetogressurs.v1.Ressurs;

/**
 * Created by ***REMOVED*** on 10.01.2017.
 */
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
