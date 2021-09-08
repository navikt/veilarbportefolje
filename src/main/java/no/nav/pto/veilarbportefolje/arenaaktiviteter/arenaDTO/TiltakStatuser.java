package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import java.util.List;

/**
 * Tiltaks topicen hjelper ogsa til med aa filtere ut de tiltakene som ikke skal i oversikten
 * @link https://confluence.adeo.no/pages/viewpage.action?pageId=409961201
 */
public class TiltakStatuser {
    public static List<String> godkjenteTiltaksStatuser = List.of("GJENN", "INFOMOETE", "JATAKK", "TILBUD", "VENTELISTE", "AKTUELL");
}
